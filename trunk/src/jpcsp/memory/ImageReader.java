/*

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
package jpcsp.memory;

import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT1;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT3;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT5;
import static jpcsp.graphics.GeCommands.CMODE_FORMAT_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.CMODE_FORMAT_16BIT_ABGR5551;
import static jpcsp.graphics.GeCommands.CMODE_FORMAT_16BIT_ABGR4444;
import static jpcsp.graphics.GeCommands.CMODE_FORMAT_32BIT_ABGR8888;
import jpcsp.graphics.GeCommands;

/**
 * @author gid15
 *
 */
public class ImageReader {
	/**
	 * Return an Image Reader implementing the IMemoryReader interface.
	 * The image is read from memory and the following formats are supported:
	 * - TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
	 * - TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
	 * - TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
	 * - TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
	 * - TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED (with clut)
	 * - TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED (with clut)
	 * - TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED (with clut)
	 * - TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED (with clut)
	 * - TPSM_PIXEL_STORAGE_MODE_DXT1
	 * - TPSM_PIXEL_STORAGE_MODE_DXT3
	 * - TPSM_PIXEL_STORAGE_MODE_DXT5
	 * - swizzled or not
	 * 
	 * A call the IMemoryReader.readNext() will return the next pixel color when
	 * reading from the top left pixel to bottom right pixel.
	 * 
	 * The pixel color is always returned in the format GU_COLOR_8888 (ABGR).
	 * 
	 * When the bufferWidth is larger than the image width, the extra pixels
	 * are automatically skipped and not returned by readNext().
	 * When the bufferWidth is smaller than the image width, a maximum of bufferWidth
	 * pixels is returned by readNext().
	 * The total number of pixels returned by readNext() is
	 *    height * Math.min(width, bufferWidth)
	 * 
	 * @param address       the address of the top left pixel of the image
	 * @param width         the width (in pixels) of the image
	 * @param height        the height (in pixels) of the image
	 * @param bufferWidth   the maximum number of pixels stored in memory for each row
	 * @param pixelFormat   the format of the pixel in memory.
	 *                      The following formats are supported:
	 *                          TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
	 *                          TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
	 *                          TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
	 *                          TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
	 *                          TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
	 *                          TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
	 *                          TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
	 *                          TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
	 *                          TPSM_PIXEL_STORAGE_MODE_DXT1
	 *                          TPSM_PIXEL_STORAGE_MODE_DXT3
	 *                          TPSM_PIXEL_STORAGE_MODE_DXT5
	 * @param swizzle       false if the image is stored sequentially in memory.
	 *                      true if the image is stored swizzled in memory.
	 * @param clutAddr      the address of the first clut element
	 * @param clutMode      the format of the clut entries
	 *                          CMODE_FORMAT_16BIT_BGR5650
	 *                          CMODE_FORMAT_16BIT_ABGR5551
	 *                          CMODE_FORMAT_16BIT_ABGR4444
	 *                          CMODE_FORMAT_32BIT_ABGR8888
	 * @param clutNumBlocks the number of clut blocks
	 * @param clutStart     the clut start index
	 * @param clutShift     the clut index shift
	 * @param clutMask      the clut index mask
	 * @return              the Image Reader implementing the IMemoryReader interface
	 */
	public static IMemoryReader getImageReader(int address, int width, int height, int bufferWidth, int pixelFormat, boolean swizzle, int clutAddr, int clutMode, int clutNumBlocks, int clutStart, int clutShift, int clutMask) {
		//
		// Step 1: read from memory as 32-bit values
		//
		int byteSize = getImageByteSize(width, height, bufferWidth, pixelFormat);
		IMemoryReader imageReader = MemoryReader.getMemoryReader(address, byteSize, 4);

		//
		// Step 2: unswizzle the memory if applicable
		//
		if (swizzle) {
			imageReader = new SwizzleDecoder(imageReader, bufferWidth, getBytesPerPixel(pixelFormat));
		}

		//
		// Step 3: split the 32-bit values to smaller values based on the pixel format
		// Step 4: convert the pixel for 32-bit values in format 8888 ABGR
		//
		boolean hasClut = false;
		boolean isCompressed = false;
		switch (pixelFormat) {
			case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
				imageReader = new Value32to16Decoder(imageReader);
				imageReader = new PixelFormat4444Decoder(imageReader);
				break;
			case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
				imageReader = new Value32to16Decoder(imageReader);
				imageReader = new PixelFormat5551Decoder(imageReader);
				break;
			case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
				imageReader = new Value32to16Decoder(imageReader);
				imageReader = new PixelFormat565Decoder(imageReader);
				break;
			case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
				break;
			case TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED:
				imageReader = new Value32to4Decoder(imageReader);
				if (clutStart == 0 && clutShift == 0 && clutMask == 0xFF) {
					imageReader = new SimpleClutDecoder(imageReader, clutAddr, clutMode, clutNumBlocks);
				} else {
					imageReader = new ClutDecoder(imageReader, clutAddr, clutMode, clutNumBlocks, clutStart, clutShift, clutMask);
				}
				hasClut = true;
				break;
			case TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED:
				imageReader = new Value32to8Decoder(imageReader);
				if (clutStart == 0 && clutShift == 0 && clutMask == 0xFF) {
					imageReader = new SimpleClutDecoder(imageReader, clutAddr, clutMode, clutNumBlocks);
				} else {
					imageReader = new ClutDecoder(imageReader, clutAddr, clutMode, clutNumBlocks, clutStart, clutShift, clutMask);
				}
				hasClut = true;
				break;
			case TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED:
				imageReader = new Value32to16Decoder(imageReader);
				if (clutStart == 0 && clutShift == 0 && clutMask == 0xFF) {
					imageReader = new SimpleClutDecoder(imageReader, clutAddr, clutMode, clutNumBlocks);
				} else {
					imageReader = new ClutDecoder(imageReader, clutAddr, clutMode, clutNumBlocks, clutStart, clutShift, clutMask);
				}
				hasClut = true;
				break;
			case TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED:
				if (clutStart == 0 && clutShift == 0 && clutMask == 0xFF) {
					imageReader = new SimpleClutDecoder(imageReader, clutAddr, clutMode, clutNumBlocks);
				} else {
					imageReader = new ClutDecoder(imageReader, clutAddr, clutMode, clutNumBlocks, clutStart, clutShift, clutMask);
				}
				hasClut = true;
				break;
			case TPSM_PIXEL_STORAGE_MODE_DXT1:
				imageReader = new DXT1Decoder(imageReader, width, height);
				isCompressed = true;
				break;
			case TPSM_PIXEL_STORAGE_MODE_DXT3:
				imageReader = new DXT3Decoder(imageReader, width, height);
				isCompressed = true;
				break;
			case TPSM_PIXEL_STORAGE_MODE_DXT5:
				imageReader = new DXT5Decoder(imageReader, width, height);
				isCompressed = true;
				break;
		}

		//
		// Step 5: convert the values produced by the clut to 32-bit values in 8888 ABGR format
		//
		if (hasClut) {
			switch (clutMode) {
				case CMODE_FORMAT_16BIT_BGR5650:
					imageReader = new PixelFormat565Decoder(imageReader);
					break;
				case CMODE_FORMAT_16BIT_ABGR5551:
					imageReader = new PixelFormat5551Decoder(imageReader);
					break;
				case CMODE_FORMAT_16BIT_ABGR4444:
					imageReader = new PixelFormat4444Decoder(imageReader);
					break;
				case CMODE_FORMAT_32BIT_ABGR8888:
					break;
			}
		}

		//
		// Step 6: remove the extra row pixels if bufferWidth is larger than width
		//
		if (!isCompressed && bufferWidth > width) {
			imageReader = new MemoryImageDecoder(imageReader, width, bufferWidth);
		}

		return imageReader;
	}

	/**
	 * The ImageReader classes are based on a decoder concept, receiving
	 * a IMemoryReader as input and delivering the transformed output also
	 * through the IMemoryReader interface.
	 * 
	 * This is the base class for all the decoders.
	 */
	private static abstract class ImageDecoder implements IMemoryReader {
		protected IMemoryReader memoryReader;

		public ImageDecoder(IMemoryReader memoryReader) {
			this.memoryReader = memoryReader;
		}

		@Override
		public void skip(int n) {
			for (int i = 0; i < n; i++) {
				readNext();
			}
		}
	}

	/**
	 * Decoder:
	 * - input: 32-bit values
	 * - output: 16-bit values (one 32-bit value is producing 2 16-bit values)
	 */
	private static final class Value32to16Decoder extends ImageDecoder {
		private int index;
		private int value;

		public Value32to16Decoder(IMemoryReader memoryReader) {
			super(memoryReader);
			index = 0;
		}

		@Override
		public int readNext() {
			if (index == 0) {
				value = memoryReader.readNext();
				index = 1;
				return (value & 0xFFFF);
			} else {
				index = 0;
				return (value >>> 16);
			}
		}
	}

	/**
	 * Decoder:
	 * - input: 32-bit values
	 * - output: 8-bit values (one 32-bit value is producing 4 8-bit values)
	 */
	private static final class Value32to8Decoder extends ImageDecoder {
		private int index;
		private int value;

		public Value32to8Decoder(IMemoryReader memoryReader) {
			super(memoryReader);
			index = 4;
		}

		@Override
		public int readNext() {
			if (index == 4) {
				index = 0;
				value = memoryReader.readNext();
			}

			int n = value & 0xFF;
			value >>= 8;
			index++;

			return n;
		}
	}

	/**
	 * Decoder:
	 * - input: 32-bit values
	 * - output: 4-bit values (one 32-bit value is producing 8 4-bit values)
	 */
	private static final class Value32to4Decoder extends ImageDecoder {
		private int index;
		private int value;

		public Value32to4Decoder(IMemoryReader memoryReader) {
			super(memoryReader);
			index = 8;
		}

		@Override
		public int readNext() {
			if (index == 8) {
				index = 0;
				value = memoryReader.readNext();
			}

			int n = value & 0xF;
			value >>= 4;
			index++;

			return n;
		}
	}

	/**
	 * Decoder:
	 * - input: 16-bit color values in 5551 ABGR format
	 * - output: 32-bit color values in 8888 ABGR format
	 */
	private static final class PixelFormat5551Decoder extends ImageDecoder {
		public PixelFormat5551Decoder(IMemoryReader memoryReader) {
			super(memoryReader);
		}

		@Override
		public int readNext() {
			return color5551to8888(memoryReader.readNext());
		}
	}

	/**
	 * Decoder:
	 * - input: 16-bit color values in 565 BGR format
	 * - output: 32-bit color values in 8888 ABGR format
	 */
	private static final class PixelFormat565Decoder extends ImageDecoder {
		public PixelFormat565Decoder(IMemoryReader memoryReader) {
			super(memoryReader);
		}

		@Override
		public int readNext() {
			return color565to8888(memoryReader.readNext());
		}
	}

	/**
	 * Decoder:
	 * - input: 16-bit color values in 4444 ABGR format
	 * - output: 32-bit color values in 8888 ABGR format
	 */
	private static final class PixelFormat4444Decoder extends ImageDecoder {
		public PixelFormat4444Decoder(IMemoryReader memoryReader) {
			super(memoryReader);
		}

		@Override
		public int readNext() {
			return color4444to8888(memoryReader.readNext());
		}
	}

	/**
	 * Decoder:
	 * - input: 32-bit values forming a swizzled image
	 * - output: 32-bit values corresponding to the unswizzled image
	 */
	private static final class SwizzleDecoder extends ImageDecoder {
		private int bufferWidth;
		private int bytesPerPixel;
		private int[] buffer;
		private int index;
		private int maxIndex;

		public SwizzleDecoder(IMemoryReader memoryReader, int bufferWidth, int bytesPerPixel) {
			super(memoryReader);
			this.bufferWidth = bufferWidth;
			this.bytesPerPixel = bytesPerPixel;

			// Swizzle buffer providing space for 8 pixel rows
			buffer = new int[bufferWidth * 8];
			maxIndex = buffer.length;
			index = maxIndex;
		}

		@Override
		public int readNext() {
			if (index >= maxIndex) {
				// Reload the swizzle buffer with the next 8 pixel rows
		        int rowWidth = (bytesPerPixel > 0) ? (bufferWidth * bytesPerPixel) : (bufferWidth / 2);
		        int pitch = (rowWidth - 16) / 4;
		        int bxc = rowWidth / 16;
		        int xdest = 0;
				for (int bx = 0; bx < bxc; bx++) {
					int dest = xdest;
					for (int n = 0; n < 8; n++) {
						buffer[dest    ] = memoryReader.readNext();
						buffer[dest + 1] = memoryReader.readNext();
						buffer[dest + 2] = memoryReader.readNext();
						buffer[dest + 3] = memoryReader.readNext();

						dest += pitch + 4;
					}
					xdest += (16 / 4);
				}

				index = 0;
			}

			return buffer[index++];
		}
	}

	/**
	 * Decoder:
	 * - input: image with size bufferWidth * height
	 * - output: image with size Math.min(bufferWidth, width) * height
	 */
	private static final class MemoryImageDecoder extends ImageDecoder {
		private int minWidth;
		private int skipWidth;
		private int x;

		public MemoryImageDecoder(IMemoryReader memoryReader, int width, int bufferWidth) {
			super(memoryReader);
			minWidth = Math.min(width, bufferWidth);
			skipWidth = Math.max(0, bufferWidth - width);
			x = 0;
		}

		@Override
		public int readNext() {
			if (x >= minWidth) {
				memoryReader.skip(skipWidth);
				x = 0;
			}
			x++;

			return memoryReader.readNext();
		}
	}

	/**
	 * Decoder for image with clut (color lookup table):
	 * - input: image in format
	 *       TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
	 *       TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
	 *       TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
	 *       TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
	 * - output: image in format
	 *       TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650  (when CMODE_FORMAT_16BIT_BGR5650)
	 *       TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551 (when CMODE_FORMAT_16BIT_ABGR5551)
	 *       TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444 (when CMODE_FORMAT_16BIT_ABGR4444)
	 *       TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888 (when CMODE_FORMAT_32BIT_ABGR8888)
	 */
	private static class ClutDecoder extends ImageDecoder {
		protected int[] clut;
		protected int clutAddr;
		protected int clutNumBlocks;
		protected int clutStart;
		protected int clutShift;
		protected int clutMask;
		protected int clutEntrySize;

		public ClutDecoder(IMemoryReader memoryReader, int clutAddr, int clutMode, int clutNumBlocks, int clutStart, int clutShift, int clutMask) {
			super(memoryReader);
			this.clutAddr = clutAddr;
			this.clutNumBlocks = clutNumBlocks;
			this.clutStart = clutStart;
			this.clutShift = clutShift;
			this.clutMask = clutMask;

			clutEntrySize = (clutMode == GeCommands.CMODE_FORMAT_32BIT_ABGR8888 ? 4 : 2);

			readClut();
		}

		protected int getClutAddr() {
			return clutAddr + clutStart * clutEntrySize;
	    }

		protected void readClut() {
			int clutNumEntries = clutNumBlocks * 32 / clutEntrySize;
			clut = new int[clutNumEntries];
			IMemoryReader clutReader = MemoryReader.getMemoryReader(getClutAddr(), (clutNumEntries - clutStart) * clutEntrySize, clutEntrySize);
			for (int i = clutStart; i < clutNumEntries; i++) {
				clut[i] = clutReader.readNext();
			}
		}

		protected int getClutIndex(int index) {
			return ((clutStart + index) >> clutShift) & clutMask;
		}

		@Override
		public int readNext() {
			int index = memoryReader.readNext();
			return clut[getClutIndex(index)];
		}
	}

	/**
	 * Decoder for image with clut (color lookup table):
	 * - input: image in format
	 *       TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
	 *       TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
	 *       TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
	 *       TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
	 * - output: image in format
	 *       TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650  (when CMODE_FORMAT_16BIT_BGR5650)
	 *       TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551 (when CMODE_FORMAT_16BIT_ABGR5551)
	 *       TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444 (when CMODE_FORMAT_16BIT_ABGR4444)
	 *       TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888 (when CMODE_FORMAT_32BIT_ABGR8888)
	 *
	 * Only "simple" cluts (the most common case) are supported
	 * by this specialized class:
	 *   clutStart = 0
	 *   clutShift = 0
	 *   clutMask = 0xFF
	 */
	private static final class SimpleClutDecoder extends ClutDecoder {
		public SimpleClutDecoder(IMemoryReader memoryReader, int clutMode, int clutAddr, int clutNumBlocks) {
			super(memoryReader, clutMode, clutAddr, clutNumBlocks, 0, 0, 0xFF);
		}

		@Override
		protected int getClutIndex(int index) {
			return index;
		}

		@Override
		public int readNext() {
			int index = memoryReader.readNext();
			return clut[index];
		}
	}

	/**
	 * Base class for the DXT-compressed decoders
	 */
	private static abstract class DXTDecoder extends ImageDecoder {
		protected int compressedImageSize;
		protected int width;
		protected int dxtLevel;
		protected int x;
		protected int[] buffer;
		protected int index;
		protected int maxIndex;
		protected int[] colors;

		public DXTDecoder(IMemoryReader memoryReader, int width, int height, int dxtLevel, int compressionRatio) {
			super(memoryReader);
			this.width = width;
			this.dxtLevel = dxtLevel;

			compressedImageSize = round4(width) * round4(height) * 4 / compressionRatio;

			buffer = new int[width * 4]; // DXT images are compressed in blocks of 4 rows
			maxIndex = width * 4;
			index = maxIndex;
			colors = new int[4];
		}

		@Override
		public int readNext() {
			if (index >= maxIndex) {
				// Reload buffer
				for (int strideX = 0; strideX < width; strideX += 4) {
                    // PSP DXT1 hardware format reverses the colors and the per-pixel
                    // bits, and encodes the color in RGB 565 format
					//
                    // PSP DXT3 format reverses the alpha and color parts of each block,
                    // and reverses the color and per-pixel terms in the color part.
					//
                    // PSP DXT5 format reverses the alpha and color parts of each block,
                    // and reverses the color and per-pixel terms in the color part. In
                    // the alpha part, the 2 reference alpha values are swapped with the
                    // alpha interpolation values.
					int bits = memoryReader.readNext();
					int color = memoryReader.readNext();
					readAlpha();

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
						// Transparent black
						r3 = 0x00;
						g3 = 0x00;
						b3 = 0x00;
					}

					colors[0] = (b0 << 16) | (g0 << 8) | (r0);
					colors[1] = (b1 << 16) | (g1 << 8) | (r1);
					colors[2] = (b2 << 16) | (g2 << 8) | (r2);
					colors[3] = (b3 << 16) | (g3 << 8) | (r3);

					storePixels(strideX, bits);
				}

				index = 0;
			}

			return buffer[index++];
		}

		protected abstract void storePixels(int strideX, int bits);
		protected abstract void readAlpha();
	}

	/**
	 * Decoder for an image compressed with DXT1
	 * (http://en.wikipedia.org/wiki/S3_Texture_Compression)
	 * - input: image in format
	 *       TPSM_PIXEL_STORAGE_MODE_DXT1
	 * - output: image in format
	 *       TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
	 */
	private static final class DXT1Decoder extends DXTDecoder {
		public DXT1Decoder(IMemoryReader memoryReader, int width, int height) {
			super(memoryReader, width, height, 1, 8);
		}

		@Override
		protected void storePixels(int strideX, int bits) {
			colors[0] |= 0xFF000000;
			colors[1] |= 0xFF000000;
			colors[2] |= 0xFF000000;
			// colors[3] is transparent black

			for (int y = 0; y < 4; y++) {
				for (int x = 0; x < 4; x++, bits >>>= 2) {
					buffer[y * width + x + strideX] = colors[bits & 3];
				}
			}
		}

		@Override
		protected void readAlpha() {
			// No alpha
		}
	}

	/**
	 * Decoder for an image compressed with DXT3
	 * (http://en.wikipedia.org/wiki/S3_Texture_Compression)
	 * - input: image in format
	 *       TPSM_PIXEL_STORAGE_MODE_DXT3
	 * - output: image in format
	 *       TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
	 */
	private static final class DXT3Decoder extends DXTDecoder {
		private long alpha;

		public DXT3Decoder(IMemoryReader memoryReader, int width, int height) {
			super(memoryReader, width, height, 3, 4);
		}

		@Override
		protected void storePixels(int strideX, int bits) {
			for (int y = 0; y < 4; y++) {
				for (int x = 0; x < 4; x++, bits >>>= 2, alpha >>>= 4) {
					int pixelAlpha = (((int) alpha) & 0xF);
					pixelAlpha = pixelAlpha | (pixelAlpha << 4);
					buffer[y * width + x + strideX] = colors[bits & 3] | (pixelAlpha << 24);
				}
			}
		}

		@Override
		protected void readAlpha() {
			alpha = memoryReader.readNext();
			alpha |= (((long) memoryReader.readNext()) << 32);
		}
	}

	/**
	 * Decoder for an image compressed with DXT5
	 * (http://en.wikipedia.org/wiki/S3_Texture_Compression)
	 * - input: image in format
	 *       TPSM_PIXEL_STORAGE_MODE_DXT5
	 * - output: image in format
	 *       TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
	 */
	private static final class DXT5Decoder extends DXTDecoder {
		private int[] alpha = new int[8];
		private long alphaLookup;

		public DXT5Decoder(IMemoryReader memoryReader, int width, int height) {
			super(memoryReader, width, height, 5, 4);
		}

		@Override
		protected void storePixels(int strideX, int bits) {
			for (int y = 0; y < 4; y++) {
				for (int x = 0; x < 4; x++, bits >>>= 2, alphaLookup >>>= 2) {
					int alphaPixel = alpha[((int) alphaLookup & 3)];
					buffer[y * width + x + strideX] = colors[bits & 3] | (alphaPixel << 24);
				}
			}
		}

		@Override
		protected void readAlpha() {
			alphaLookup = memoryReader.readNext();
			int value = memoryReader.readNext();
			alphaLookup |= ((long) (value & 0xFFFF)) << 32;
			value >>>= 16;
			int alpha0 = value & 0xFF;
			int alpha1 = value >> 8;

			alpha[0] = alpha0;
			alpha[1] = alpha1;
			if (alpha0 > alpha1) {
				alpha[2] = (6 * alpha0 +     alpha1) / 7;
				alpha[3] = (5 * alpha0 + 2 * alpha1) / 7;
				alpha[4] = (4 * alpha0 + 3 * alpha1) / 7;
				alpha[5] = (3 * alpha0 + 4 * alpha1) / 7;
				alpha[6] = (2 * alpha0 + 5 * alpha1) / 7;
				alpha[7] = (    alpha0 + 6 * alpha1) / 7;
			} else {
				alpha[2] = (4 * alpha0 +     alpha1) / 5;
				alpha[3] = (3 * alpha0 + 2 * alpha1) / 5;
				alpha[4] = (2 * alpha0 + 3 * alpha1) / 5;
				alpha[5] = (    alpha0 + 4 * alpha1) / 5;
				alpha[6] = 0x00;
				alpha[7] = 0xFF;
			}
		}
	}

	/**
	 * Convert a 4444 color in ABGR format (GU_COLOR_4444)
	 * to a 8888 color in ABGR format (GU_COLOR_8888).
	 *
	 *     4444 format: AAAABBBBGGGGRRRR
	 *                  3210321032103210
	 * transformed into
	 *     8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
	 *                  32103210321032103210321032103210
	 *
	 * @param color4444 4444 color in ABGR format (GU_COLOR_4444)
	 * @return          8888 color in ABGR format (GU_COLOR_8888)
	 */
	public static int color4444to8888(int color4444) {
		return ((color4444      ) & 0x0000000F) |
		       ((color4444 <<  4) & 0x00000FF0) |
               ((color4444 <<  8) & 0x000FF000) |
               ((color4444 << 12) & 0x0FF00000) |
               ((color4444 << 16) & 0xF0000000);
	}

	/**
	 * Convert a 5551 color in ABGR format (GU_COLOR_5551)
	 * to a 8888 color in ABGR format (GU_COLOR_8888).
	 *
	 *     5551 format: ABBBBBGGGGGRRRRR
	 *                   432104321043210
	 * transformed into
	 *     8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
	 *                          432104324321043243210432
	 *
	 * @param color5551 5551 color in ABGR format (GU_COLOR_5551)
	 * @return          8888 color in ABGR format (GU_COLOR_8888)
	 */
	public static int color5551to8888(int color5551) {
		return ((color5551 <<  3) & 0x000000F8) |
               ((color5551 >>  2) & 0x00000007) |
               ((color5551 <<  6) & 0x0000F800) |
               ((color5551 <<  1) & 0x00000700) |
               ((color5551 <<  9) & 0x00F80000) |
               ((color5551 <<  4) & 0x00070000) |
               ((color5551 >> 15) * 0xFF000000);
	}

	/**
	 * Convert a 565 color in BGR format (GU_COLOR_5650)
	 * to a 8888 color in ABGR format (GU_COLOR_8888).
	 *
	 *     5650 format: BBBBBGGGGGGRRRRR
	 *                  4321054321043210
	 * transformed into
	 *     8888 format: 11111111BBBBBBBBGGGGGGGGRRRRRRRR
	 *                          432104325432105443210432
	 *
	 * @param color565 565 color in BGR format (GU_COLOR_5650)
	 * @return         8888 color in ABGR format (GU_COLOR_8888)
	 */
	public static int color565to8888(int color565) {
		return ((color565 << 3) & 0x0000F8) |
               ((color565 >> 2) & 0x000007) |
               ((color565 << 5) & 0x00FC00) |
               ((color565 >> 1) & 0x000300) |
               ((color565 << 8) & 0xF80000) |
               ((color565 << 3) & 0x070000) |
               0xFF000000;
	}

	/**
	 * Convert a 8888 color from ABGR (PSP format) to ARGB (Java/Swing format).
	 *     ABGR: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
	 * transformed into
	 *     ARGB: AAAAAAAARRRRRRRRGGGGGGGGBBBBBBBB
	 *
	 * @param colorABGR 8888 color in ABGR format
	 * @return          8888 color in ARGB format
	 */
	public static int colorABGRtoARGB(int colorABGR) {
		return ((colorABGR & 0xFF00FF00)      ) |
		       ((colorABGR & 0x000000FF) << 16) |
		       ((colorABGR & 0x00FF0000) >> 16);
	}

	/**
	 * Return the number of bytes required by a pixel in a specific format.
	 * 
	 * @param pixelFormat the format of the pixel (TPSM_PIXEL_STORAGE_MODE_xxx)
	 * @return            the number of bytes required by one pixel.
	 *                    0 when a pixel requires a half-byte (i.e. 2 pixels per byte).
	 */
	public static int getBytesPerPixel(int pixelFormat) {
		switch (pixelFormat) {
			case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
			case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
			case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
				return 2;
			case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
				return 4;
			case TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED:
				return 0; // meaning 0.5 byte per pixel
			case TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED:
				return 1;
			case TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED:
				return 2;
			case TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED:
				return 4;
			case TPSM_PIXEL_STORAGE_MODE_DXT1:
				return 0; // meaning 0.5 byte per pixel
			case TPSM_PIXEL_STORAGE_MODE_DXT3:
			case TPSM_PIXEL_STORAGE_MODE_DXT5:
				return 1;
		}

		return -1;
	}

	/**
	 * Return the image size in bytes when stored in Memory.
	 * 
	 * @param width       the image width
	 * @param height      the image height
	 * @param bufferWidth the image bufferWidth
	 * @param pixelFormat the image pixelFormat
	 * @return            number of bytes required to store the image in Memory
	 */
	public static int getImageByteSize(int width, int height, int bufferWidth, int pixelFormat) {
		// Special cases:
		switch (pixelFormat) {
			case TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED:
				return height * bufferWidth / 2;
			case TPSM_PIXEL_STORAGE_MODE_DXT1:
				return round4(height) * round4(width) / 2;
			case TPSM_PIXEL_STORAGE_MODE_DXT3:
			case TPSM_PIXEL_STORAGE_MODE_DXT5:
				return round4(height) * round4(width);
		}

		// Common case:
		return height * bufferWidth * getBytesPerPixel(pixelFormat);
	}

	/**
	 * Round the value the next multiple of 4.
	 * E.g.: value == 0: return 0
	 *       value == 1, 2, 3, 4: return 4
	 *       value == 5, 6, 7, 8: return 8
	 *
	 * @param n the value
	 * @return  the value rounded up to the next multiple of 4.
	 */
	public static int round4(int n) {
		return (n + 3) & ~3;
	}
}
