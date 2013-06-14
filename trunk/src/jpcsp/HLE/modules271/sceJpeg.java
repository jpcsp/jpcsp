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
import static jpcsp.graphics.RE.software.ImageWriter.color8888to4444;
import static jpcsp.memory.ImageReader.color4444to8888;
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
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
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
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

@HLELogging
public class sceJpeg extends HLEModule {
    public static Logger log = Modules.getLogger("sceJpeg");

    private static class MemoryWriter2Bits implements IMemoryWriter {
    	private final IMemoryWriter memoryWriter;
    	private int index;
    	private int byteValue;

    	public MemoryWriter2Bits(int address) {
    		memoryWriter = MemoryWriter.getMemoryWriter(address, 1);
    	}

    	@Override
		public void writeNext(int value) {
    		byteValue |= (value & 0x03) << index;
    		index += 2;

    		if (index == 8) {
    			memoryWriter.writeNext(byteValue);
    			byteValue = 0;
    			index = 0;
    		}
		}

		@Override
		public void skip(int n) {
			memoryWriter.skip(n);
		}

		@Override
		public void flush() {
			memoryWriter.flush();
			index = 0;
			byteValue = 0;
		}

		@Override
		public int getCurrentAddress() {
			return memoryWriter.getCurrentAddress();
		}
    }

    private static class MemoryReader2Bits implements IMemoryReader {
    	private final IMemoryReader memoryReader;
    	private int index;
    	private int byteValue;

    	public MemoryReader2Bits(int address) {
    		memoryReader = MemoryReader.getMemoryReader(address, 1);
    	}

		@Override
		public int readNext() {
			if (index == 0) {
				byteValue = memoryReader.readNext();
			}
			int value = (byteValue >> index) & 0x03;
			index = (index + 2) & 7;

			return value;
		}

		@Override
		public void skip(int n) {
			memoryReader.skip(n);
			index = 0;
		}

		@Override
		public int getCurrentAddress() {
			return memoryReader.getCurrentAddress();
		}
    }

    @Override
    public String getName() {
        return "sceJpeg";
    }

    protected static final int PSP_JPEG_MJPEG_DHT_MODE = 0;
    protected static final int PSP_JPEG_MJPEG_NO_DHT_MODE = 1;

    protected int jpegWidth = Screen.width;
    protected int jpegHeight = Screen.height;
    protected HashMap<Integer, BufferedImage> bufferedImages;
    protected static final String uidPurpose = "sceJpeg-BufferedImage";
    protected static final boolean dumpJpegFile = false;

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

    private static int colorYCbCrToABGR(int yCbCr) {
    	return color4444to8888(yCbCr) | 0xFF000000;
    }

    private static int colorARGBToYCbCr(int argb) {
    	return color8888to4444(colorARGBtoABGR(argb & 0x00FFFFFF));
    }

    protected static BufferedImage readJpegImage(TPointer jpegBuffer, int jpegBufferSize) {
        BufferedImage bufferedImage = null;
        byte[] buffer = readJpegImageBytes(jpegBuffer, jpegBufferSize);

        if (dumpJpegFile) {
            dumpJpegFile(jpegBuffer, jpegBufferSize);
        }

        InputStream imageInputStream = new ByteArrayInputStream(buffer);
        try {
            bufferedImage = ImageIO.read(imageInputStream);
            imageInputStream.close();
        } catch (IOException e) {
            log.error("Error reading Jpeg image", e);
        }

        return bufferedImage;
    }

    protected static int getWidthHeight(int width, int height) {
        return (width << 16) | height;
    }

    protected static int getWidth(int widthHeight) {
        return (widthHeight >> 16) & 0xFFF;
    }

    protected static int getHeight(int widthHeight) {
        return widthHeight & 0xFFF;
    }

    protected static byte[] readJpegImageBytes(TPointer jpegBuffer, int jpegBufferSize) {
        byte[] buffer = new byte[jpegBufferSize];
        IMemoryReader memoryReader = MemoryReader.getMemoryReader(jpegBuffer.getAddress(), jpegBufferSize, 1);
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) memoryReader.readNext();
        }

        return buffer;
    }

    protected static void dumpJpegFile(TPointer jpegBuffer, int jpegBufferSize) {
        byte[] buffer = readJpegImageBytes(jpegBuffer, jpegBufferSize);
        try {
            OutputStream os = new FileOutputStream(String.format("%s%cImage%08X.jpeg", Settings.getInstance().readString("emu.tmppath"), File.separatorChar, jpegBuffer.getAddress()));
            os.write(buffer);
            os.close();
        } catch (IOException e) {
            log.error("Error dumping Jpeg file", e);
        }
    }

    protected void decodeImage(TPointer imageBuffer, BufferedImage bufferedImage, int width, int height, int bufferWidth, int pixelFormat, int startLine) {
        width = Math.min(width, bufferedImage.getWidth());
        height = Math.min(height, bufferedImage.getHeight());

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

    public int hleGetYCbCrBufferSize(BufferedImage bufferedImage) {
        // Return necessary buffer size for conversion: 12 bits per pixel
        return ((bufferedImage.getWidth() * bufferedImage.getHeight()) >> 1) * 3;
    }

    public int hleJpegDecodeYCbCr(BufferedImage bufferedImage, TPointer yCbCrBuffer, int yCbCrBufferSize, int dhtMode) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        int address1 = yCbCrBuffer.getAddress();
        int address2 = address1 + width * height;
        int address3 = address2 + ((width * height) >> 2);
        if (log.isDebugEnabled()) {
        	log.debug(String.format("hleJpegDecodeYCbCr 0x%08X, 0x%08X, 0x%08X", address1, address2, address3));
        }
        IMemoryWriter imageWriter1 = MemoryWriter.getMemoryWriter(address1, yCbCrBufferSize, 1);
        IMemoryWriter imageWriter2 = new MemoryWriter2Bits(address2);
        IMemoryWriter imageWriter3 = new MemoryWriter2Bits(address3);
    	for (int y = 0; y < height; y++) {
    		for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                int yCbCr = colorARGBToYCbCr(argb);
                imageWriter1.writeNext(yCbCr & 0xFF);
                imageWriter2.writeNext(yCbCr >> 8);
                imageWriter3.writeNext(yCbCr >> 10);
    		}
    	}
    	imageWriter1.flush();
    	imageWriter2.flush();
    	imageWriter3.flush();
    	
        return getWidthHeight(width, height);
    }

    protected int hleJpegDecodeMJpegYCbCr(TPointer jpegBuffer, int jpegBufferSize, TPointer yCbCrBuffer, int yCbCrBufferSize, int dhtMode) {
        BufferedImage bufferedImage = readJpegImage(jpegBuffer, jpegBufferSize);
        if (bufferedImage == null) {
        	yCbCrBuffer.clear(yCbCrBufferSize);
        	return getWidthHeight(0, 0);
        }

        return hleJpegDecodeYCbCr(bufferedImage, yCbCrBuffer, yCbCrBufferSize, dhtMode);
    }

    protected int hleJpegCsc(TPointer imageBuffer, TPointer yCbCrBuffer, int widthHeight, int bufferWidth) {
        int height = getHeight(widthHeight);
        int width = getWidth(widthHeight);

        int pixelFormat = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
        int bytesPerPixel = sizeOfTextureType[pixelFormat];
        int lineWidth = Math.min(width, bufferWidth);
        int skipEndOfLine = Math.max(0, bufferWidth - lineWidth);
        int imageSizeInBytes = height * bufferWidth * bytesPerPixel;
        IMemoryWriter imageWriter = MemoryWriter.getMemoryWriter(imageBuffer.getAddress(), imageSizeInBytes, bytesPerPixel);

        int address1 = yCbCrBuffer.getAddress();
        int address2 = address1 + width * height;
        int address3 = address2 + ((width * height) >> 2);
        if (log.isDebugEnabled()) {
        	log.debug(String.format("hleJpegCsc 0x%08X, 0x%08X, 0x%08X", address1, address2, address3));
        }
        IMemoryReader imageReader1 = MemoryReader.getMemoryReader(address1, 1);
        IMemoryReader imageReader2 = new MemoryReader2Bits(address2);
        IMemoryReader imageReader3 = new MemoryReader2Bits(address3);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
            	int yCbCr = imageReader1.readNext() | (imageReader2.readNext() << 8) | (imageReader3.readNext() << 10);

            	// Convert yCbCr to ABGR
            	int abgr = colorYCbCrToABGR(yCbCr);

            	// Write ABGR
                imageWriter.writeNext(abgr);
            }
            imageWriter.skip(skipEndOfLine);
        }
        imageWriter.flush();

        VideoEngine.getInstance().addVideoTexture(imageBuffer.getAddress(), imageBuffer.getAddress() + imageSizeInBytes);

        return 0;
    }

    @HLEFunction(nid = 0x04B5AE02, version = 271)
    public int sceJpegMJpegCsc(TPointer imageBuffer, TPointer yCbCrBuffer, int widthHeight, int bufferWidth) {
    	return hleJpegCsc(imageBuffer, yCbCrBuffer, widthHeight, bufferWidth);
    }

    /**
     * Deletes the current decoder context.
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x48B602B7, version = 271)
    public int sceJpegDeleteMJpeg() {
        return 0;
    }

    /**
     * Finishes the MJpeg library
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x7D2F3D7F, version = 271)
    public int sceJpegFinishMJpeg() {
        return 0;
    }

    @HLEFunction(nid = 0x91EED83C, version = 271)
    public int sceJpegDecodeMJpegYCbCr(TPointer jpegBuffer, int jpegBufferSize, TPointer yCbCrBuffer, int yCbCrBufferSize, int dhtMode) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("sceJpegDecodeMJpegYCbCr jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize)));
        }

        return hleJpegDecodeMJpegYCbCr(jpegBuffer, jpegBufferSize, yCbCrBuffer, yCbCrBufferSize, dhtMode);
    }

    /**
     * Creates the decoder context.
     *
     * @param width - The width of the frame
     * @param height - The height of the frame
     *
     * @return 0 on success, < 0 on error
     */
    @HLELogging(level = "info")
    @HLEFunction(nid = 0x9D47469C, version = 271)
    public int sceJpegCreateMJpeg(int width, int height) {
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
        return 0;
    }

    /**
     * Decodes a mjpeg frame.
     *
     * @param jpegbuf - the buffer with the mjpeg frame
     * @param size - size of the buffer pointed by jpegbuf
     * @param rgba - buffer where the decoded data in RGBA format will be
     * stored. It should have a size of (width * height * 4).
     * @param dht - flag telling if this mjpeg has a DHT (Define Huffman Table)
     * header or not.
     *
     * @return (width << 16) + height on success, < 0 on error
     */
    @HLEFunction(nid = 0x04B93CEF, version = 271)
    public int sceJpegDecodeMJpeg(TPointer jpegBuffer, int jpegBufferSize, TPointer imageBuffer, int dhtMode) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("sceJpegDecodeMJpeg jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize)));
        }

        int pixelFormat = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
        BufferedImage bufferedImage = readJpegImage(jpegBuffer, jpegBufferSize);
        if (bufferedImage == null) {
            generateFakeImage(imageBuffer, jpegWidth, jpegHeight, jpegWidth, pixelFormat);
        } else {
            decodeImage(imageBuffer, bufferedImage, jpegWidth, jpegHeight, jpegWidth, pixelFormat, 0);
        }

        // Return size of image
        return getWidthHeight(jpegWidth, jpegHeight);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8F2BB012, version = 271)
    public int sceJpegGetOutputInfo(TPointer jpegBuffer, int jpegBufferSize, @CanBeNull TPointer32 colorInfoBuffer, int dhtMode) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("sceJpegGetOutputInfo jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize)));
        }

        if (dumpJpegFile) {
            dumpJpegFile(jpegBuffer, jpegBufferSize);
        }

        // Buffer to store info about the color space in use.
        // - Bits 24 to 32 (Always empty): 0x00
        // - Bits 16 to 24 (Color mode): 0x00 (Unknown), 0x01 (Greyscale) or 0x02 (YCbCr) 
        // - Bits 8 to 16 (Vertical chroma subsampling value): 0x00, 0x01 or 0x02
        // - Bits 0 to 8 (Horizontal chroma subsampling value): 0x00, 0x01 or 0x02
        if (colorInfoBuffer.isNotNull()) {
            colorInfoBuffer.setValue(0x00020202);
        }

        BufferedImage bufferedImage = readJpegImage(jpegBuffer, jpegBufferSize);
        if (bufferedImage == null) {
        	return 0xC000;
        }

        return hleGetYCbCrBufferSize(bufferedImage);
    }

    /**
     * Used in relation with sceMpegAvcConvertToYuv420. Maybe
     * converting a Yuv420 image to ABGR888?
     *
     * @param imageBuffer
     * @param yCbCrBuffer
     * @param widthHeight
     * @param bufferWidth
     * @param colorInfo
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x67F0ED84, version = 271)
    public int sceJpegCsc(TPointer imageBuffer, TPointer yCbCrBuffer, int widthHeight, int bufferWidth, int colorInfo) {
    	return hleJpegCsc(imageBuffer, yCbCrBuffer, widthHeight, bufferWidth);
    }

    @HLEFunction(nid = 0x64B6F978, version = 271)
    public int sceJpegDecodeMJpegSuccessively(TPointer jpegBuffer, int jpegBufferSize, TPointer imageBuffer, int dhtMode) {
        // Works in the same way as sceJpegDecodeMJpeg, but sends smaller blocks to the Media Engine in a real PSP (avoids speed decrease).
        if (log.isTraceEnabled()) {
            log.trace(String.format("sceJpegDecodeMJpegSuccessively jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize)));
        }

        int pixelFormat = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
        BufferedImage bufferedImage = readJpegImage(jpegBuffer, jpegBufferSize);
        int width = jpegWidth;
        int height = jpegHeight;
        if (bufferedImage == null) {
            generateFakeImage(imageBuffer, jpegWidth, jpegHeight, jpegWidth, pixelFormat);
        } else {
            decodeImage(imageBuffer, bufferedImage, jpegWidth, jpegHeight, jpegWidth, pixelFormat, 0);
            width = bufferedImage.getWidth();
            height = bufferedImage.getHeight();
        }

        return getWidthHeight(width, height);
    }
    
    @HLEFunction(nid = 0x227662D7, version = 271)
    public int sceJpegDecodeMJpegYCbCrSuccessively(TPointer jpegBuffer, int jpegBufferSize, TPointer yCbCrBuffer, int yCbCrBufferSize, int dhtMode) {
        // Works in the same way as sceJpegDecodeMJpegYCbCr, but sends smaller blocks to the Media Engine in a real PSP (avoids speed decrease).
        if (log.isTraceEnabled()) {
            log.trace(String.format("sceJpegDecodeMJpegYCbCrSuccessively jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize)));
        }

        return hleJpegDecodeMJpegYCbCr(jpegBuffer, jpegBufferSize, yCbCrBuffer, yCbCrBufferSize, dhtMode);
    }
}