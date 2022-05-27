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
package jpcsp.graphics.capture;

import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT1;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT3;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT5;
import static jpcsp.graphics.RE.IRenderingEngine.RE_DEPTH_COMPONENT;
import static jpcsp.graphics.RE.IRenderingEngine.RE_DEPTH_STENCIL;
import static jpcsp.graphics.RE.software.BaseRenderer.depthBufferPixelFormat;
import static jpcsp.memory.ImageReader.colorABGRtoARGB;
import static jpcsp.util.Utilities.alignUp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/**
 * @author gid15
 *
 */
public class CaptureImage {
	private static Logger log = CaptureManager.log;
	private static final String bmpFileFormat = "bmp";
	private static final String pngFileFormat = "png";
	private int imageaddr;
	private int level;
	private Buffer buffer;
	private IMemoryReader imageReader;
	private int width;
	private int height;
	private int bufferWidth;
	private int bufferStorage;
	private boolean compressedImage;
	private int compressedImageSize;
	private boolean invert;
	private boolean overwriteFile;
	private String fileNamePrefix;
	private String directory = "tmp/";
	private String fileFormat = bmpFileFormat;
	private String fileName;
	private String fileNameSuffix = "";
	private static HashMap<Integer, Integer> lastFileIndex = new HashMap<Integer, Integer>();

	private static abstract class AbstractCaptureImage {
		public abstract void writeHeader(String fileName, String fileFormat, int width, int height, int readWidth) throws IOException;
		public abstract void startLine(int y);
		public abstract void writePixel(int pixel) throws IOException;
		public abstract void writeEnd() throws IOException;

		public void writePixel(byte[] pixel) throws IOException {
			writePixel(getARGB(pixel));
		}

		public void endLine() throws IOException {
		}

		public boolean isInverted() {
			return false;
		}
	}

	// See http://en.wikipedia.org/wiki/BMP_file_format
	// for detailed information about the BMP file format
	private static class CaptureImageBMP extends AbstractCaptureImage {
		private int imageRawBytes;
		private byte[] completeImageBytes;
		private int pixelIndex;
		private int width;
		private int height;
		private String fileName;

		@Override
		public void writeHeader(String fileName, String fileFormat, int width, int height, int readWidth) throws IOException {
			this.fileName = fileName;
			this.width = width;
			this.height = height;
			int rowPad = (4 - ((width * 4) & 3)) & 3;
			imageRawBytes = (width * 4) + rowPad;

			completeImageBytes = new byte[height * imageRawBytes];
		}


		@Override
		public void startLine(int y) {
			pixelIndex = y * imageRawBytes;
		}

		@Override
		public void writePixel(int pixel) {
			completeImageBytes[pixelIndex + 0] = (byte) (pixel >> 16); // B
			completeImageBytes[pixelIndex + 1] = (byte) (pixel >>  8); // G
			completeImageBytes[pixelIndex + 2] = (byte) (pixel      ); // R
			completeImageBytes[pixelIndex + 3] = (byte) (pixel >> 24); // A

			pixelIndex += 4;
		}

		@Override
		public void writeEnd() throws IOException {
			byte[] fileHeader = new byte[14];
			byte[] dibHeader = new byte[56];
			int fileSize = fileHeader.length + dibHeader.length + completeImageBytes.length;

			fileHeader[0] = 'B';                                  // Magic number
			fileHeader[1] = 'M';                                  // Magic number
			storeLittleEndianInt(fileHeader, 2, fileSize);        // Size of the BMP file
			storeLittleEndianInt(fileHeader, 10, fileHeader.length + dibHeader.length); // Offset where the Pixel Array (bitmap data) can be found

			storeLittleEndianInt(dibHeader, 0, dibHeader.length); // Number of bytes in the DIB header (from this point)
			storeLittleEndianInt(dibHeader, 4, width);            // Width of the bitmap in pixels
			storeLittleEndianInt(dibHeader, 8, height);           // Height of the bitmap in pixels
			storeLittleEndianShort(dibHeader, 12, 1);             // Number of color planes being used
			storeLittleEndianShort(dibHeader, 14, 32);            // Number of bits per pixel
			storeLittleEndianInt(dibHeader, 16, 0);               // BI_BITFIELDS, no Pixel Array compression used
			storeLittleEndianInt(dibHeader, 20, completeImageBytes.length); // Size of the raw data in the Pixel Array (including padding)
			storeLittleEndianInt(dibHeader, 24, 2835);            // Horizontal physical resolution of the image (pixels/meter)
			storeLittleEndianInt(dibHeader, 28, 2835);            // Vertical physical resolution of the image (pixels/meter)
			storeLittleEndianInt(dibHeader, 32, 0);               // Number of colors in the palette
			storeLittleEndianInt(dibHeader, 36, 0);               // 0 means all colors are important
			storeLittleEndianInt(dibHeader, 40, 0x00FF0000);      // Red channel bit mask in big-endian (valid because BI_BITFIELDS is specified)
			storeLittleEndianInt(dibHeader, 44, 0x0000FF00);      // Green channel bit mask in big-endian (valid because BI_BITFIELDS is specified)
			storeLittleEndianInt(dibHeader, 48, 0x000000FF);      // Blue channel bit mask in big-endian (valid because BI_BITFIELDS is specified)
			storeLittleEndianInt(dibHeader, 52, 0xFF000000);      // Alpha channel bit mask in big-endian

			OutputStream outStream = new FileOutputStream(fileName);
			outStream.write(fileHeader);
			outStream.write(dibHeader);
			outStream.write(completeImageBytes);
			outStream.close();
		}

		@Override
		public boolean isInverted() {
			// The image in the BMP file has always to be upside-down as compared to the PSP image
			return true;
		}
	}

	private static class CaptureImageImageIO extends AbstractCaptureImage {
		private BufferedImage im;
		private int[] lineARGB;
		private int x;
		private int y;
		private int width;
		private String fileName;
		private String fileFormat;

		@Override
		public void writeHeader(String fileName, String fileFormat, int width, int height, int readWidth) throws IOException {
			this.fileName = fileName;
			this.fileFormat = fileFormat;
			this.width = width;

			// Remark: use TYPE_3BYTE_BGR instead of TYPE_4BYTE_ABGR, it looks like ImageIO
			// is not correctly handling images with alpha values. Incorrect png and jpg images
			// are created when using an alpha component.
			im = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			lineARGB = new int[width];
		}

		@Override
		public void startLine(int y) {
			this.y = y;
			x = 0;
		}

		@Override
		public void writePixel(int pixel) throws IOException {
			lineARGB[x] = colorABGRtoARGB(pixel);
			x++;
		}

		@Override
		public void endLine() {
			im.setRGB(0, y, width, 1, lineARGB, 0, width);
		}

		@Override
		public void writeEnd() throws IOException {
			if (!ImageIO.write(im, fileFormat, new File(fileName))) {
				log.error(String.format("Cannot save image in format %s using ImageIO: %s", fileFormat, fileName));
			}
		}
	}

	private static class CaptureImagePNG extends AbstractCaptureImage {
		private int width;
		private int height;
		private String fileName;
		private byte[] buffer;
		private int pixelIndex;

		@Override
		public void writeHeader(String fileName, String fileFormat, int width, int height, int readWidth) throws IOException {
			this.fileName = fileName;
			this.width = width;
			this.height = height;

			// 4 bytes per pixel plus one byte per line for the filter type
			buffer = new byte[width * height * 4 + height];
		}

		@Override
		public void startLine(int y) {
			pixelIndex = width * y * 4 + y;

			// Write the filter type byte at the beginning of each line
			buffer[pixelIndex] = 0; // filter type: None
			pixelIndex++;
		}

		@Override
		public void writePixel(int pixel) throws IOException {
			buffer[pixelIndex + 0] = (byte) (pixel      ); // R
			buffer[pixelIndex + 1] = (byte) (pixel >>  8); // G
			buffer[pixelIndex + 2] = (byte) (pixel >> 16); // B
			buffer[pixelIndex + 3] = (byte) (pixel >> 24); // A

			pixelIndex += 4;
		}

		@Override
		public void writeEnd() throws IOException {
			// See https://en.wikipedia.org/wiki/Portable_Network_Graphics
			// for detailed information about the PNG file format

			byte[] fileHeader = new byte[8];
			fileHeader[0] = (byte) 0x89;
			fileHeader[1] = 'P';
			fileHeader[2] = 'N';
			fileHeader[3] = 'G';
			fileHeader[4] = '\r';
			fileHeader[5] = '\n';
			fileHeader[6] = (byte) 0x1A;
			fileHeader[7] = '\n';

			byte[] ihdr = new byte[13 + 8 + 4];
			storeBigEndianInt(ihdr, 0, 13);
			storeChunkType(ihdr, 4, 'I', 'H', 'D', 'R');
			storeBigEndianInt(ihdr, 8, width);
			storeBigEndianInt(ihdr, 12, height);
			ihdr[16] = 8; // bit depth
			ihdr[17] = 6; // color type: red, green, blue, alpha
			ihdr[18] = 0; // compression method: deflate/inflate
			ihdr[19] = 0; // filter method: none
			ihdr[20] = 0; // interlace method: no interlace
			storeCRC(ihdr, 21);

			Deflater deflater = new Deflater();
			deflater.setInput(buffer);
			deflater.finish();
			byte[] data = new byte[buffer.length];
			int dataLength = deflater.deflate(data);
			byte[] idat = new byte[8 + dataLength + 4];
			storeBigEndianInt(idat, 0, dataLength);
			storeChunkType(idat, 4, 'I', 'D', 'A', 'T');
			System.arraycopy(data, 0, idat, 8, dataLength);
			storeCRC(idat, 8 + dataLength);

			byte[] iend = new byte[12];
			storeBigEndianInt(iend, 0, 0);
			storeChunkType(iend, 4, 'I', 'E', 'N', 'D');
			storeCRC(iend, 8);

			OutputStream outStream = new FileOutputStream(fileName);
			outStream.write(fileHeader);
			outStream.write(ihdr);
			outStream.write(idat);
			outStream.write(iend);
			outStream.close();
		}
	}

	public CaptureImage(int imageaddr, int level, Buffer buffer, int width, int height, int bufferWidth, int bufferStorage, boolean compressedImage, int compressedImageSize, boolean invert, boolean overwriteFile, String fileNamePrefix) {
		this.imageaddr = imageaddr;
		this.level = level;
		this.buffer = buffer;
		this.width = width;
		this.height = height;
		this.bufferWidth = bufferWidth;
		this.bufferStorage = bufferStorage;
		this.compressedImage = compressedImage;
		this.compressedImageSize = compressedImageSize;
		this.invert = invert;
		this.overwriteFile = overwriteFile;
		this.fileNamePrefix = fileNamePrefix == null ? "Image" : fileNamePrefix;
	}

	public CaptureImage(int imageaddr, int level, IMemoryReader imageReader, int width, int height, int bufferWidth, boolean invert, boolean overwriteFile, String fileNamePrefix) {
		this.imageaddr = imageaddr;
		this.level = level;
		this.imageReader = imageReader;
		this.width = width;
		this.height = height;
		this.bufferWidth = bufferWidth;
		this.bufferStorage = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
		this.compressedImage = false;
		this.compressedImageSize = 0;
		this.invert = invert;
		this.overwriteFile = overwriteFile;
		this.fileNamePrefix = fileNamePrefix == null ? "Image" : fileNamePrefix;
	}

	public void setFileFormat(String fileFormat) {
		this.fileFormat = fileFormat;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getFileName() {
		if (fileName == null) {
	    	String levelName = "";
	    	if (level > 0) {
	    		levelName = "_" + level;
	    	}

	    	int scanIndex = 0;
	    	Integer lastIndex = lastFileIndex.get(imageaddr);
	    	if (lastIndex != null) {
	    		scanIndex = lastIndex.intValue() + 1;
	    	}
	    	for (int i = scanIndex; ; i++) {
	    		String id = (i == 0 ? "" : "-" + i);
	    		fileName = String.format("%s%s%08X%s%s%s.%s", directory, fileNamePrefix, imageaddr, fileNameSuffix, levelName, id, fileFormat);
	    		if (overwriteFile) {
	    			break;
	    		}

	    		File file = new File(fileName);
	    		if (!file.exists()) {
	    			lastFileIndex.put(imageaddr, i);
	    			break;
	    		}
	    	}
		}

		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean fileExists() {
		return new File(getFileName()).exists();
	}

	public void setFileNameSuffix(String fileNameSuffix) {
		this.fileNameSuffix = fileNameSuffix;
	}

	private ShortBuffer getShortBuffer() {
		if (buffer instanceof ShortBuffer) {
			return (ShortBuffer) buffer;
		}
		if (buffer instanceof ByteBuffer) {
			return ((ByteBuffer) buffer).asShortBuffer();
		}

		return null;
	}

	private IntBuffer getIntBuffer() {
		if (buffer instanceof IntBuffer) {
			return (IntBuffer) buffer;
		}
		if (buffer instanceof ByteBuffer) {
			return ((ByteBuffer) buffer).asIntBuffer();
		}

		return null;
	}

	private boolean isShortBuffer() {
		return getShortBuffer() != null;
	}

	private boolean isIntBuffer() {
		return getIntBuffer() != null;
	}

	public void write() throws IOException {
    	if (bufferStorage >= TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED && bufferStorage <= TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED && bufferStorage != depthBufferPixelFormat) {
    		// Writing of indexed images not supported
    		return;
    	}

    	if (compressedImage) {
    		decompressImage();
    	}

		boolean imageInvert = invert;

		int readWidth = Math.min(width, bufferWidth);
		byte[] pixelBytes = new byte[4];
		byte[] blackPixelBytes = new byte[pixelBytes.length];

		// ImageIO doesn't support the bmp file format and
		// doesn't properly write PNG files with pixel alpha values
		AbstractCaptureImage captureImage;
		if (bmpFileFormat.equals(fileFormat)) {
			captureImage = new CaptureImageBMP();
		} else if (pngFileFormat.equals(fileFormat)) {
			captureImage = new CaptureImagePNG();
		} else {
			captureImage = new CaptureImageImageIO();
		}

		captureImage.writeHeader(getFileName(), fileFormat, width, height, readWidth);
		if (captureImage.isInverted()) {
			imageInvert = !imageInvert;
		}

		boolean imageType32Bit = bufferStorage == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888 || bufferStorage == RE_DEPTH_STENCIL;
		if (imageReader != null) {
			for (int y = 0; y < height; y++) {
				captureImage.startLine(imageInvert ? (height - y - 1) : y);
				for (int x = 0; x < readWidth; x++) {
					int pixel = imageReader.readNext();
					captureImage.writePixel(pixel);
				}
				captureImage.endLine();
			}
			captureImage.writeEnd();
		} else if (isIntBuffer() && imageType32Bit) {
    		IntBuffer intBuffer = getIntBuffer();
			for (int y = 0; y < height; y++) {
				intBuffer.position((imageInvert ? (height - y - 1) : y) * bufferWidth);
				captureImage.startLine(imageInvert ? (height - y - 1) : y);
				for (int x = 0; x < readWidth; x++) {
					try {
						int pixel = intBuffer.get();
						captureImage.writePixel(pixel);
					} catch (BufferUnderflowException e) {
						captureImage.writePixel(blackPixelBytes);
					}
				}
				captureImage.endLine();
			}
    	} else if (isShortBuffer() && !imageType32Bit) {
    		ShortBuffer shortBuffer = getShortBuffer();
			for (int y = 0; y < height; y++) {
				shortBuffer.position((imageInvert ? (height - y - 1) : y) * bufferWidth);
				captureImage.startLine(imageInvert ? (height - y - 1) : y);
				for (int x = 0; x < readWidth; x++) {
					short pixel = shortBuffer.get();
					getPixelBytes(pixel, bufferStorage, pixelBytes);
					captureImage.writePixel(pixelBytes);
				}
				captureImage.endLine();
			}
    	} else if (isIntBuffer() && !imageType32Bit) {
    		IntBuffer intBuffer = getIntBuffer();
			for (int y = 0; y < height; y++) {
				intBuffer.position((imageInvert ? (height - y - 1) : y) * bufferWidth / 2);
				captureImage.startLine(imageInvert ? (height - y - 1) : y);
				for (int x = 0; x < readWidth; x += 2) {
					try {
						int twoPixels = intBuffer.get();
						getPixelBytes((short) twoPixels, bufferStorage, pixelBytes);
						captureImage.writePixel(pixelBytes);
						if (x + 1 < readWidth) {
							getPixelBytes((short) (twoPixels >>> 16), bufferStorage, pixelBytes);
							captureImage.writePixel(pixelBytes);
						}
					} catch (BufferUnderflowException e) {
						captureImage.writePixel(blackPixelBytes);
						captureImage.writePixel(blackPixelBytes);
					}
				}
				captureImage.endLine();
			}
    	} else if (imageType32Bit) {
			for (int y = 0; y < height; y++) {
	    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(imageaddr + (imageInvert ? (height - y - 1) : y) * bufferWidth * 4, bufferWidth * 4, 4);
				captureImage.startLine(imageInvert ? (height - y - 1) : y);
				for (int x = 0; x < readWidth; x++) {
					int pixel = memoryReader.readNext();
					captureImage.writePixel(pixel);
				}
				captureImage.endLine();
			}
    	} else {
			for (int y = 0; y < height; y++) {
	    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(imageaddr + (imageInvert ? (height - y - 1) : y) * bufferWidth * 2, bufferWidth * 2, 2);
				captureImage.startLine(imageInvert ? (height - y - 1) : y);
				for (int x = 0; x < readWidth; x++) {
					short pixel = (short) memoryReader.readNext();
					getPixelBytes(pixel, bufferStorage, pixelBytes);
					captureImage.writePixel(pixelBytes);
				}
				captureImage.endLine();
			}
    	}

		if (buffer != null) {
			buffer.rewind();
		}
		captureImage.writeEnd();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Saved image to %s", getFileName()));
		}
    }

    private static void storeLittleEndianInt(byte[] buffer, int offset, int value) {
    	buffer[offset    ] = (byte) (value      );
    	buffer[offset + 1] = (byte) (value >>  8);
    	buffer[offset + 2] = (byte) (value >> 16);
    	buffer[offset + 3] = (byte) (value >> 24);
    }

    private static void storeBigEndianInt(byte[] buffer, int offset, int value) {
    	buffer[offset    ] = (byte) (value >> 24);
    	buffer[offset + 1] = (byte) (value >> 16);
    	buffer[offset + 2] = (byte) (value >>  8);
    	buffer[offset + 3] = (byte) (value      );
    }

    private static void storeChunkType(byte[] buffer, int offset, char c1, char c2, char c3, char c4) {
    	buffer[offset    ] = (byte) c1;
    	buffer[offset + 1] = (byte) c2;
    	buffer[offset + 2] = (byte) c3;
    	buffer[offset + 3] = (byte) c4;
    }

    private static void storeCRC(byte[] buffer, int offset) {
    	CRC32 crc32 = new CRC32();
    	crc32.update(buffer, 4, offset - 4);
    	storeBigEndianInt(buffer, offset, (int) crc32.getValue());
    }

    private static void storeLittleEndianShort(byte[] buffer, int offset, int value) {
    	buffer[offset    ] = (byte) (value      );
    	buffer[offset + 1] = (byte) (value >>  8);
    }

    private void getPixelBytes(short pixel, int imageType, byte[] pixelBytes) {
    	switch (imageType) {
	    	case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
	    		pixelBytes[0] = (byte) ((pixel >> 8) & 0xF8); // B
	    		pixelBytes[1] = (byte) ((pixel >> 3) & 0xFC); // G
	    		pixelBytes[2] = (byte) ((pixel << 3) & 0xF8); // R
	    		pixelBytes[3] = 0;                            // A
	    		break;
	    	case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
	    		pixelBytes[0] = (byte) ((pixel >> 7) & 0xF8); // B
	    		pixelBytes[1] = (byte) ((pixel >> 2) & 0xF8); // G
	    		pixelBytes[2] = (byte) ((pixel << 3) & 0xF8); // R
	    		pixelBytes[3] = (byte) ((pixel >> 15) != 0 ? 0xFF : 0x00); // A
	    		break;
	    	case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
	    		pixelBytes[0] = (byte) ((pixel >> 4) & 0xF0); // B
	    		pixelBytes[1] = (byte) ((pixel     ) & 0xF0); // G
	    		pixelBytes[2] = (byte) ((pixel << 4) & 0xF0); // R
	    		pixelBytes[3] = (byte) ((pixel >> 8) & 0xF0); // A
	    		break;
	    	case RE_DEPTH_COMPONENT:
	    	case depthBufferPixelFormat:
	    		// Gray color value based on depth value
	    		pixelBytes[0] = (byte) (pixel >> 8);
	    		pixelBytes[1] = pixelBytes[0];
	    		pixelBytes[2] = pixelBytes[0];
	    		pixelBytes[3] = pixelBytes[0];
	    		break;
			default:
				// Black pixel
				pixelBytes[0] = 0;
				pixelBytes[1] = 0;
				pixelBytes[2] = 0;
				pixelBytes[3] = 0;
				break;
    	}
    }

    private static int getARGB(byte[] pixelBytes) {
    	return ((pixelBytes[3] & 0xFF) << 24) |
    	       ((pixelBytes[2] & 0xFF) << 16) |
    	       ((pixelBytes[1] & 0xFF) << 8) |
    	       ((pixelBytes[0] & 0xFF));
    }

    private void storePixel(IntBuffer buffer, int x, int y, int color) {
    	buffer.put(y * width + x, color);
    }

    private int round4(int n) {
    	return alignUp(n, 3);
    }

    private int getInt32(Buffer buffer) {
    	if (buffer instanceof IntBuffer) {
    		return ((IntBuffer) buffer).get();
    	} else if (buffer instanceof ShortBuffer) {
    		ShortBuffer shortBuffer = (ShortBuffer) buffer;
    		int n0 = shortBuffer.get() & 0xFFFF;
    		int n1 = shortBuffer.get() & 0xFFFF;
    		return (n1 << 16) | n0;
    	} else if (buffer instanceof ByteBuffer) {
    		return ((ByteBuffer) buffer).getInt();
    	}

    	return 0;
    }

    private void decompressImageDXT(int dxtLevel) {
		IntBuffer decompressedBuffer = IntBuffer.allocate(round4(width) * round4(height));

		//
		// For more information of the S3 Texture compression (DXT), see
		// http://en.wikipedia.org/wiki/S3_Texture_Compression
		//
		int strideX = 0;
		int strideY = 0;
		int[] colors = new int[4];
		int strideSize = (dxtLevel == 1 ? 8 : 16);
		int[] alphas = new int[16];
		int[] alphasLookup = new int[8];
		for (int i = 0; i < compressedImageSize; i += strideSize) {
			if (dxtLevel > 1) {
				if (dxtLevel <= 3) {
					// 64 bits of alpha channel data: four bits for each pixel
					int alphaBits = 0;
					for (int j = 0; j < 16; j++, alphaBits >>>= 4) {
						if ((j % 8) == 0) {
							alphaBits = getInt32(buffer);
						}
						int alpha = alphaBits & 0x0F;
						alphas[j] = alpha << 4;
					}
				} else {
					// 64 bits of alpha channel data: two 8 bit alpha values and a 4x4 3 bit lookup table
					int bits0 = getInt32(buffer);
					int bits1 = getInt32(buffer);
					int alpha0 = bits0 & 0xFF;
					int alpha1 = (bits0 >> 8) & 0xFF;
					alphasLookup[0] = alpha0;
					alphasLookup[1] = alpha1;
					if (alpha0 > alpha1) {
						alphasLookup[2] = (6 * alpha0 + 1 * alpha1) / 7;
						alphasLookup[3] = (5 * alpha0 + 2 * alpha1) / 7;
						alphasLookup[4] = (4 * alpha0 + 3 * alpha1) / 7;
						alphasLookup[5] = (3 * alpha0 + 4 * alpha1) / 7;
						alphasLookup[6] = (2 * alpha0 + 5 * alpha1) / 7;
						alphasLookup[7] = (1 * alpha0 + 6 * alpha1) / 7;
					} else {
						alphasLookup[2] = (4 * alpha0 + 1 * alpha1) / 5;
						alphasLookup[3] = (3 * alpha0 + 2 * alpha1) / 5;
						alphasLookup[4] = (2 * alpha0 + 3 * alpha1) / 5;
						alphasLookup[5] = (1 * alpha0 + 4 * alpha1) / 5;
						alphasLookup[6] = 0x00;
						alphasLookup[7] = 0xFF;
					}
					int bits = bits0 >> 16;
					for (int j = 0; j < 16; j++) {
						int lookup;
						if (j == 5) {
							lookup = (bits & 1) << 2 | (bits1 & 3);
							bits = bits1 >>> 2;
						} else {
							lookup = bits & 7;
							bits >>>= 3;
						}
						alphas[j] = alphasLookup[lookup];
					}
				}
			}
			int color = getInt32(buffer);
			int color0 = (color >>  0) & 0xFFFF;
			int color1 = (color >> 16) & 0xFFFF;

			int r0 = (color0 >> 8) & 0xF8;
			int g0 = (color0 >> 3) & 0xFC;
			int b0 = (color0 << 3) & 0xF8;

			int r1 = (color1 >> 8) & 0xF8;
			int g1 = (color1 >> 3) & 0xFC;
			int b1 = (color1 << 3) & 0xF8;

			int r2, g2, b2;
			if (color0 > color1 || dxtLevel > 1) {
				r2 = (r0 * 2 + r1) / 3;
				g2 = (g0 * 2 + g1) / 3;
				b2 = (b0 * 2 + b1) / 3;
			} else {
				r2 = (r0 + r1) / 2;
				g2 = (g0 + g1) / 2;
				b2 = (b0 + b1) / 2;
			}

			int r3, g3, b3;
			if (color0 > color1 || dxtLevel > 1) {
				r3 = (r0 + r1 * 2) / 3;
				g3 = (g0 + g1 * 2) / 3;
				b3 = (b0 + b1 * 2) / 3;
			} else {
				r3 = 0x00;
				g3 = 0x00;
				b3 = 0x00;
			}

			colors[0] = ((b0 & 0xFF) << 16) | ((g0 & 0xFF) << 8) | (r0 & 0xFF);
			colors[1] = ((b1 & 0xFF) << 16) | ((g1 & 0xFF) << 8) | (r1 & 0xFF);
			colors[2] = ((b2 & 0xFF) << 16) | ((g2 & 0xFF) << 8) | (r2 & 0xFF);
			colors[3] = ((b3 & 0xFF) << 16) | ((g3 & 0xFF) << 8) | (r3 & 0xFF);

			int bits = getInt32(buffer);
			for (int y = 0, alphaIndex = 0; y < 4; y++) {
				for (int x = 0; x < 4; x++, bits >>>= 2, alphaIndex++) {
					int bgr = colors[bits & 3];
					int alpha = alphas[alphaIndex] << 24;
					storePixel(decompressedBuffer, strideX + x, strideY + y, bgr | alpha);
				}
			}

			strideX += 4;
			if (strideX >= width) {
				strideX = 0;
				strideY += 4;
			}
		}

		buffer.rewind();
		compressedImage = false;
		buffer = decompressedBuffer;
		bufferWidth = width;
		bufferStorage = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
    }

    private void decompressImage() {
    	switch (bufferStorage) {
			case TPSM_PIXEL_STORAGE_MODE_DXT1:
				decompressImageDXT(1);
				break;
			case TPSM_PIXEL_STORAGE_MODE_DXT3:
				decompressImageDXT(3);
				break;
			case TPSM_PIXEL_STORAGE_MODE_DXT5:
				decompressImageDXT(5);
				break;
			default:
				log.warn(String.format("Unsupported compressed buffer storage %d", bufferStorage));
				break;
		}
    }
}
