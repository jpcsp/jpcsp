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

import static jpcsp.graphics.RE.software.BaseRenderer.depthBufferPixelFormat;

import java.io.BufferedOutputStream;
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

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/**
 * @author gid15
 *
 */
public class CaptureImage {
	private int imageaddr;
	private int level;
	private Buffer buffer;
	private int width;
	private int height;
	private int bufferWidth;
	private int bufferStorage;
	private boolean compressedImage;
	private int compressedImageSize;
	private boolean invert;
	private boolean overwriteFile;
	private String fileNamePrefix;
	private static HashMap<Integer, Integer> lastFileIndex = new HashMap<Integer, Integer>();

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

    public void write() throws IOException {
    	if (bufferStorage >= GeCommands.TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED && bufferStorage <= GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED && bufferStorage != depthBufferPixelFormat) {
    		// Writing of indexed images not supported
    		return;
    	}

    	String levelName = "";
    	if (level > 0) {
    		levelName = "_" + level;
    	}

    	String fileName = null;
    	int scanIndex = 0;
    	Integer lastIndex = lastFileIndex.get(imageaddr);
    	if (lastIndex != null) {
    		scanIndex = lastIndex.intValue() + 1;
    	}
    	for (int i = scanIndex; ; i++) {
    		String id = (i == 0 ? "" : "-" + i);
    		fileName = String.format("tmp/%s%08X%s%s.bmp", fileNamePrefix, imageaddr, levelName, id);
    		if (overwriteFile) {
    			break;
    		}

    		File file = new File(fileName);
    		if (!file.exists()) {
    			lastFileIndex.put(imageaddr, i);
    			break;
    		}
    	}

    	if (compressedImage) {
    		decompressImage();
    	}

		if (width > bufferWidth) {
			width = bufferWidth;
		}

		// Unfortunately, I was not able to generate the image file
		// using the ImageIO API :-(
		// This is why I'm generating a BMP file manually...
		//
		// See http://en.wikipedia.org/wiki/BMP_file_format
		// for detailed information about the BMP file format
		//
		byte[] fileHeader = new byte[14];
		byte[] dibHeader = new byte[56];
		int rowPad = (4 - ((width * 4) & 3)) & 3;
		int imageSize = height * ((width * 4) + rowPad);
		int fileSize = fileHeader.length + dibHeader.length + imageSize;
		OutputStream outBmp = new BufferedOutputStream(new FileOutputStream(fileName), fileSize);

		fileHeader[0] = 'B';                                  // Magic number
		fileHeader[1] = 'M';                                  // Magic number
		storeLittleEndianInt(fileHeader, 2, fileSize);        // Size of the BMP file
		storeLittleEndianInt(fileHeader, 10, fileHeader.length + dibHeader.length); // Offset where the Pixel Array (bitmap data) can be found

		storeLittleEndianInt(dibHeader, 0, dibHeader.length); // Number of bytes in the DIB header (from this point)
		storeLittleEndianInt(dibHeader, 4, width);            // Width of the bitmap in pixels
		storeLittleEndianInt(dibHeader, 8, -height);          // Height of the bitmap in pixels
		storeLittleEndianShort(dibHeader, 12, 1);             // Number of color planes being used
		storeLittleEndianShort(dibHeader, 14, 32);            // Number of bits per pixel
		storeLittleEndianInt(dibHeader, 16, 3);               // BI_BITFIELDS, no Pixel Array compression used
		storeLittleEndianInt(dibHeader, 20, imageSize);       // Size of the raw data in the Pixel Array (including padding)
		storeLittleEndianInt(dibHeader, 24, 2835);            // Horizontal physical resolution of the image (pixels/meter)
		storeLittleEndianInt(dibHeader, 28, 2835);            // Vertical physical resolution of the image (pixels/meter)
		storeLittleEndianInt(dibHeader, 32, 0);               // Number of colors in the palette
		storeLittleEndianInt(dibHeader, 36, 0);               // 0 means all colors are important
		storeLittleEndianInt(dibHeader, 40, 0x00FF0000);      // Red channel bit mask in big-endian (valid because BI_BITFIELDS is specified)
		storeLittleEndianInt(dibHeader, 44, 0x0000FF00);      // Green channel bit mask in big-endian (valid because BI_BITFIELDS is specified)
		storeLittleEndianInt(dibHeader, 48, 0x000000FF);      // Blue channel bit mask in big-endian (valid because BI_BITFIELDS is specified)
		storeLittleEndianInt(dibHeader, 52, 0xFF000000);      // Alpha channel bit mask in big-endian

		outBmp.write(fileHeader);
		outBmp.write(dibHeader);
		byte[] rowPadBytes = new byte[rowPad];
		byte[] pixelBytes = new byte[4];
		byte[] blackPixelBytes = new byte[pixelBytes.length];
		boolean imageType32Bit = bufferStorage == GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
    	if (buffer instanceof IntBuffer && imageType32Bit) {
    		IntBuffer intBuffer = (IntBuffer) buffer;
			for (int y = 0; y < height; y++) {
				intBuffer.position((invert ? (height - y - 1) : y) * bufferWidth);
				for (int x = 0; x < width; x++) {
					try {
						int pixel = intBuffer.get();
						pixelBytes[0] = (byte) (pixel >> 16); // B
						pixelBytes[1] = (byte) (pixel >>  8); // G
						pixelBytes[2] = (byte) (pixel      ); // R
						pixelBytes[3] = (byte) (pixel >> 24); // A
						outBmp.write(pixelBytes);
					} catch (BufferUnderflowException e) {
						outBmp.write(blackPixelBytes);
					}
				}
				outBmp.write(rowPadBytes);
			}
    	} else if (buffer instanceof IntBuffer && !imageType32Bit) {
    		IntBuffer intBuffer = (IntBuffer) buffer;
			for (int y = 0; y < height; y++) {
				intBuffer.position((invert ? (height - y - 1) : y) * bufferWidth / 2);
				for (int x = 0; x < width; x += 2) {
					try {
						int twoPixels = intBuffer.get();
						getPixelBytes((short) twoPixels, bufferStorage, pixelBytes);
						outBmp.write(pixelBytes);
						getPixelBytes((short) (twoPixels >>> 16), bufferStorage, pixelBytes);
						outBmp.write(pixelBytes);
					} catch (BufferUnderflowException e) {
						outBmp.write(blackPixelBytes);
						outBmp.write(blackPixelBytes);
					}
				}
				outBmp.write(rowPadBytes);
			}
    	} else if (buffer instanceof ShortBuffer && !imageType32Bit) {
    		ShortBuffer shortBuffer = (ShortBuffer) buffer;
			for (int y = 0; y < height; y++) {
				shortBuffer.position((invert ? (height - y - 1) : y) * bufferWidth);
				for (int x = 0; x < width; x++) {
					short pixel = shortBuffer.get();
					getPixelBytes(pixel, bufferStorage, pixelBytes);
					outBmp.write(pixelBytes);
				}
				outBmp.write(rowPadBytes);
			}
    	} else if (imageType32Bit) {
			for (int y = 0; y < height; y++) {
	    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(imageaddr + (invert ? (height - y - 1) : y) * bufferWidth * 4, bufferWidth * 4, 4);
				for (int x = 0; x < width; x++) {
					int pixel = memoryReader.readNext();
					pixelBytes[0] = (byte) (pixel >> 16); // B
					pixelBytes[1] = (byte) (pixel >>  8); // G
					pixelBytes[2] = (byte) (pixel      ); // R
					pixelBytes[3] = (byte) (pixel >> 24); // A
					outBmp.write(pixelBytes);
				}
				outBmp.write(rowPadBytes);
			}
    	} else {
			for (int y = 0; y < height; y++) {
	    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(imageaddr + (invert ? (height - y - 1) : y) * bufferWidth * 2, bufferWidth * 2, 2);
				for (int x = 0; x < width; x++) {
					short pixel = (short) memoryReader.readNext();
					getPixelBytes(pixel, bufferStorage, pixelBytes);
					outBmp.write(pixelBytes);
				}
				outBmp.write(rowPadBytes);
			}
    	}
    	buffer.rewind();
		outBmp.close();

        VideoEngine.log.debug(String.format("Saved image to %s", fileName));
    }

    private void storeLittleEndianInt(byte[] buffer, int offset, int value) {
    	buffer[offset    ] = (byte) (value      );
    	buffer[offset + 1] = (byte) (value >>  8);
    	buffer[offset + 2] = (byte) (value >> 16);
    	buffer[offset + 3] = (byte) (value >> 24);
    }

    private void storeLittleEndianShort(byte[] buffer, int offset, int value) {
    	buffer[offset    ] = (byte) (value      );
    	buffer[offset + 1] = (byte) (value >>  8);
    }

    private void getPixelBytes(short pixel, int imageType, byte[] pixelBytes) {
    	switch (imageType) {
    	case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
    		pixelBytes[0] = (byte) ((pixel >> 8) & 0xF8); // B
    		pixelBytes[1] = (byte) ((pixel >> 3) & 0xFC); // G
    		pixelBytes[2] = (byte) ((pixel << 3) & 0xF8); // R
    		pixelBytes[3] = 0;                            // A
    		break;
    	case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
    		pixelBytes[0] = (byte) ((pixel >> 7) & 0xF8); // B
    		pixelBytes[1] = (byte) ((pixel >> 2) & 0xF8); // G
    		pixelBytes[2] = (byte) ((pixel << 3) & 0xF8); // R
    		pixelBytes[3] = (byte) ((pixel >> 15) != 0 ? 0xFF : 0x00); // A
    		break;
    	case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
    		pixelBytes[0] = (byte) ((pixel >> 4) & 0xF0); // B
    		pixelBytes[1] = (byte) ((pixel     ) & 0xF0); // G
    		pixelBytes[2] = (byte) ((pixel << 4) & 0xF0); // R
    		pixelBytes[3] = (byte) ((pixel >> 8) & 0xF0); // A
    		break;
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

    private void storePixel(IntBuffer buffer, int x, int y, int color) {
    	buffer.put(y * width + x, color);
    }

    private int round4(int n) {
    	return (n + 3) & ~3;
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
			if (color0 > color1) {
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
		bufferStorage = GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
    }

    private void decompressImage() {
    	switch (bufferStorage) {
		case GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT1: {
			decompressImageDXT(1);
			break;
		}

		case GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT3: {
			decompressImageDXT(3);
			break;
		}

		case GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT5: {
			decompressImageDXT(5);
			break;
		}

		default:
			VideoEngine.log.warn("Unsupported compressed buffer storage " + bufferStorage);
			break;
		}
    }
}
