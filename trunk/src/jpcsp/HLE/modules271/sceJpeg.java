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
import jpcsp.graphics.RE.software.PixelColor;
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

    private static int clamp(float value) {
    	return clamp8bit((int) value);
    }

    public static int clamp8bit(int value) {
    	return Math.min(0xFF, Math.max(0, value));
    }

    private static int colorYCbCrToABGR(int y, int cb, int cr) {
		// based on http://en.wikipedia.org/wiki/Yuv#Y.27UV444_to_RGB888_conversion
    	cb -= 128;
    	cr -= 128;
		int r = clamp8bit(y + cr + (cr >> 2) + (cr >> 3) + (cr >> 5));
		int g = clamp8bit(y - ((cb >> 2) + (cb >> 4) + (cb >> 5)) - ((cr >> 1) + (cr >> 3) + (cr >> 4) + (cr >> 5)));
		int b = clamp8bit(y + cb + (cb >> 1) + (cb >> 2) + (cb >> 6));
    	return PixelColor.getColorBGR(b, g, r) | 0xFF000000;
    }

    private static int colorARGBToYCbCr(int argb) {
    	int r = (argb >> 16) & 0xFF;
    	int g = (argb >> 8) & 0xFF;
    	int b = argb & 0xFF;
    	int y = clamp(0.299f * r + 0.587f * g + 0.114f * b);
    	int cb = clamp(-0.169f * r - 0.331f * g + 0.499f * b + 128f);
    	int cr = clamp(0.499f * r - 0.418f * g - 0.0813f * b + 128f);
    	return PixelColor.getColorBGR(y, cb, cr);
    }

    private static int getY(int ycbcr) {
    	return PixelColor.getBlue(ycbcr);
    }

    private static int getCb(int ycbcr) {
    	return PixelColor.getGreen(ycbcr);
    }

    private static int getCr(int ycbcr) {
    	return PixelColor.getRed(ycbcr);
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

    /**
     * Convert an image to YUV420p format.
     *
     * See http://en.wikipedia.org/wiki/Yuv#Y.27UV420p_.28and_Y.27V12_or_YV12.29_to_RGB888_conversion
     * for the description of the YUV420p format:
     *
     * "Y'UV420p is a planar format, meaning that the Y', U, and V values are grouped together instead of interspersed.
     *  The reason for this is that by grouping the U and V values together, the image becomes much more compressible.
     *  When given an array of an image in the Y'UV420p format, all the Y' values come first,
     *  followed by all the U values, followed finally by all the V values.
     *  
     *  As with most Y'UV formats, there are as many Y' values as there are pixels.
     *  Where X equals the height multiplied by the width,
     *  the first X indices in the array are Y' values that correspond to each individual pixel.
     *  However, there are only one fourth as many U and V values.
     *  The U and V values correspond to each 2 by 2 block of the image,
     *  meaning each U and V entry applies to four pixels. After the Y' values,
     *  the next X/4 indices are the U values for each 2 by 2 block,
     *  and the next X/4 indices after that are the V values that also apply to each 2 by 2 block.
     *
     *		size.total = size.width * size.height;
     *		y = yuv[position.y * size.width + position.x];
     *		u = yuv[(position.y / 2) * (size.width / 2) + (position.x / 2) + size.total];
     *		v = yuv[(position.y / 2) * (size.width / 2) + (position.x / 2) + size.total + (size.total / 4)];
     *		rgb = Y'UV444toRGB888(y, u, v);
     * "
     *
     * @param bufferedImage		the source image.
     * @param yCbCrBuffer		the destination image in YUV420p format.
     * @param yCbCrBufferSize	the size of the destination buffer.
     * @param dhtMode			unknown.
     * @return					the width & height of the image.
     */
    public int hleJpegDecodeYCbCr(BufferedImage bufferedImage, TPointer yCbCrBuffer, int yCbCrBufferSize, int dhtMode) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        int sizeY = width * height;
        int sizeCb = sizeY >> 2;
        int addressY = yCbCrBuffer.getAddress();
        int addressCb = addressY + sizeY;
        int addressCr = addressCb + sizeCb;
        if (log.isDebugEnabled()) {
        	log.debug(String.format("hleJpegDecodeYCbCr 0x%08X, 0x%08X, 0x%08X", addressY, addressCb, addressCr));
        }

        // Store all the Cb and Cr values into an array as they will not be accessed sequentially.
        int[] bufferCb = new int[sizeCb];
        int[] bufferCr = new int[sizeCb];
        IMemoryWriter imageWriterY = MemoryWriter.getMemoryWriter(addressY, sizeY, 1);
    	for (int y = 0; y < height; y++) {
        	int indexCb = (y >> 1) * (width >> 1);
    		for (int x = 0; x < width; x += 2, indexCb++) {
                int argb0 = bufferedImage.getRGB(x, y);
                int yCbCr0 = colorARGBToYCbCr(argb0);
                int argb1 = bufferedImage.getRGB(x + 1, y);
                int yCbCr1 = colorARGBToYCbCr(argb1);
                imageWriterY.writeNext(getY(yCbCr0));
                imageWriterY.writeNext(getY(yCbCr1));

                bufferCb[indexCb] += getCb(yCbCr0);
                bufferCb[indexCb] += getCb(yCbCr1);
                bufferCr[indexCb] += getCr(yCbCr0);
                bufferCr[indexCb] += getCr(yCbCr1);
    		}
    	}
    	imageWriterY.flush();

    	IMemoryWriter imageWriterCb = MemoryWriter.getMemoryWriter(addressCb, sizeCb, 1);
        IMemoryWriter imageWriterCr = MemoryWriter.getMemoryWriter(addressCr, sizeCb, 1);
        for (int i = 0; i < sizeCb; i++) {
        	// 4 pixel values have been written for each Cb and Cr value, average them.
        	imageWriterCb.writeNext(bufferCb[i] >> 2);
        	imageWriterCr.writeNext(bufferCr[i] >> 2);
        }
    	imageWriterCb.flush();
    	imageWriterCr.flush();
    	
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

    /**
     * Convert an image in YUV420p format to ABGR8888.
     *
     * See http://en.wikipedia.org/wiki/Yuv#Y.27UV420p_.28and_Y.27V12_or_YV12.29_to_RGB888_conversion
     * for the description of the YUV420p format:
     *
     * "Y'UV420p is a planar format, meaning that the Y', U, and V values are grouped together instead of interspersed.
     *  The reason for this is that by grouping the U and V values together, the image becomes much more compressible.
     *  When given an array of an image in the Y'UV420p format, all the Y' values come first,
     *  followed by all the U values, followed finally by all the V values.
     *  
     *  As with most Y'UV formats, there are as many Y' values as there are pixels.
     *  Where X equals the height multiplied by the width,
     *  the first X indices in the array are Y' values that correspond to each individual pixel.
     *  However, there are only one fourth as many U and V values.
     *  The U and V values correspond to each 2 by 2 block of the image,
     *  meaning each U and V entry applies to four pixels. After the Y' values,
     *  the next X/4 indices are the U values for each 2 by 2 block,
     *  and the next X/4 indices after that are the V values that also apply to each 2 by 2 block.
     *
     *		size.total = size.width * size.height;
     *		y = yuv[position.y * size.width + position.x];
     *		u = yuv[(position.y / 2) * (size.width / 2) + (position.x / 2) + size.total];
     *		v = yuv[(position.y / 2) * (size.width / 2) + (position.x / 2) + size.total + (size.total / 4)];
     *		rgb = Y'UV444toRGB888(y, u, v);
     * "
     *
     * @param imageBuffer	output image in ABGR8888 format.
     * @param yCbCrBuffer   input image in YUV420p format.
     * @param widthHeight   width & height of the image
     * @param bufferWidth   buffer width of the image
     * @return               0
     */
    protected int hleJpegCsc(TPointer imageBuffer, TPointer yCbCrBuffer, int widthHeight, int bufferWidth) {
        int height = getHeight(widthHeight);
        int width = getWidth(widthHeight);

        int pixelFormat = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
        int bytesPerPixel = sizeOfTextureType[pixelFormat];
        int lineWidth = Math.min(width, bufferWidth);
        int skipEndOfLine = Math.max(0, bufferWidth - lineWidth);
        int imageSizeInBytes = height * bufferWidth * bytesPerPixel;
        IMemoryWriter imageWriter = MemoryWriter.getMemoryWriter(imageBuffer.getAddress(), imageSizeInBytes, bytesPerPixel);

        int sizeY = width * height;
        int sizeCb = sizeY >> 2;
        int addressY = yCbCrBuffer.getAddress();
        int addressCb = addressY + sizeY;
        int addressCr = addressCb + sizeCb;
        if (log.isDebugEnabled()) {
        	log.debug(String.format("hleJpegCsc 0x%08X, 0x%08X, 0x%08X", addressY, addressCb, addressCr));
        }

        // Read all the Cb and Cr values into an array as they will not be accessed sequentially.
        int[] bufferCb = new int[sizeCb];
        int[] bufferCr = new int[sizeCb];
        IMemoryReader imageReaderCb = MemoryReader.getMemoryReader(addressCb, sizeCb, 1);
        IMemoryReader imageReaderCr = MemoryReader.getMemoryReader(addressCr, sizeCb, 1);
        for (int i = 0; i < sizeCb; i++) {
        	bufferCb[i] = imageReaderCb.readNext();
        	bufferCr[i] = imageReaderCr.readNext();
        }

        IMemoryReader imageReaderY = MemoryReader.getMemoryReader(addressY, sizeY, 1);
        for (int y = 0; y < height; y++) {
        	int indexCb = (y >> 1) * (width >> 1);
            for (int x = 0; x < width; x += 2, indexCb++) {
            	int y0 = imageReaderY.readNext();
            	int y1 = imageReaderY.readNext();
            	int cb = bufferCb[indexCb];
            	int cr = bufferCr[indexCb];

            	// Convert yCbCr to ABGR
            	int abgr0 = colorYCbCrToABGR(y0, cb, cr);
            	int abgr1 = colorYCbCrToABGR(y1, cb, cr);

            	// Write ABGR
                imageWriter.writeNext(abgr0);
                imageWriter.writeNext(abgr1);
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

    @HLEUnimplemented
    @HLEFunction(nid = 0xA06A75C4, version = 271)
    public int sceJpeg_A06A75C4(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9B36444C, version = 271)
    public int sceJpeg_9B36444C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0425B986, version = 271)
    public int sceJpegDecompressAllImage() {
    	return 0;
    }
}