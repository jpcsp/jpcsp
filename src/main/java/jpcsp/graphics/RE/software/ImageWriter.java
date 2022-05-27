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
package jpcsp.graphics.RE.software;

import static jpcsp.memory.ImageReader.color4444to8888;
import static jpcsp.memory.ImageReader.color5551to8888;
import static jpcsp.memory.ImageReader.color565to8888;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.memory.IMemoryReaderWriter;
import jpcsp.memory.MemoryReaderWriter;

/**
 * @author gid15
 *
 */
public class ImageWriter {
	public static IMemoryReaderWriter getImageWriter(int address, int width, int bufferWidth, int pixelFormat) {
		int step = IRenderingEngine.sizeOfTextureType[pixelFormat];
		IMemoryReaderWriter imageWriter = MemoryReaderWriter.getMemoryReaderWriter(address, step);

		switch (pixelFormat) {
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
				imageWriter = new PixelFormat4444Encoder(imageWriter);
				break;
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
				imageWriter = new PixelFormat5551Encoder(imageWriter);
				break;
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
				imageWriter = new PixelFormat565Encoder(imageWriter);
				break;
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
				// We can use directly the MemoryReaderWriter, no format convertion needed.
				break;
			case BaseRenderer.depthBufferPixelFormat:
				// We can use directly the MemoryReaderWriter, no format convertion needed.
				break;
		}

		if (bufferWidth > width) {
			imageWriter = new MemoryImageWriter(imageWriter, width, bufferWidth);
		}

		return imageWriter;
	}

	private static final class MemoryImageWriter implements IMemoryReaderWriter {
		protected IMemoryReaderWriter imageWriter;
		private int minWidth;
		private int skipWidth;
		private int x;

		public MemoryImageWriter(IMemoryReaderWriter imageWriter, int width, int bufferWidth) {
			this.imageWriter = imageWriter;
			minWidth = Math.min(width, bufferWidth);
			skipWidth = Math.max(0, bufferWidth - width);
			x = 0;
		}

		@Override
		public void writeNext(int value) {
			imageWriter.writeNext(value);
			x++;
			if (x >= minWidth) {
				imageWriter.skip(skipWidth);
				x = 0;
			}
		}

		@Override
		public void skip(int n) {
			if (n > 0) {
				x += n;
				while (x >= minWidth) {
					n += skipWidth;
					x -= minWidth;
				}
				imageWriter.skip(n);
			}
		}

		@Override
		public void flush() {
			imageWriter.flush();
		}

		@Override
		public int readCurrent() {
			return imageWriter.readCurrent();
		}

		@Override
		public int getCurrentAddress() {
			return imageWriter.getCurrentAddress();
		}
	}

	/**
	 * Convert a 8888 color in ABGR format (GU_COLOR_8888)
	 * to a 4444 color in ABGR format (GU_COLOR_4444).
	 *
	 *     8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
	 *                  76543210765432107654321076543210
	 * transformed into
	 *     4444 format: AAAABBBBGGGGRRRR
	 *                  7654765476547654
	 *
	 * @param color8888 8888 color in ABGR format (GU_COLOR_8888)
	 * @return          4444 color in ABGR format (GU_COLOR_4444)
	 */
	public static int color8888to4444(int color8888) {
		return ((color8888 >>  4) & 0x0000000F) |
		       ((color8888 >>  8) & 0x000000F0) |
		       ((color8888 >> 12) & 0x00000F00) |
		       ((color8888 >> 16) & 0x0000F000);
	}

	/**
	 * Convert a 8888 color in ABGR format (GU_COLOR_8888)
	 * to a 5551 color in ABGR format (GU_COLOR_5551).
	 *
	 *     8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
	 *                  76543210765432107654321076543210
	 * transformed into
	 *     5551 format: ABBBBBGGGGGRRRRR
	 *                  7765437654376543
	 *
	 * @param color8888 8888 color in ABGR format (GU_COLOR_8888)
	 * @return          5551 color in ABGR format (GU_COLOR_5551)
	 */
	public static int color8888to5551(int color8888) {
		return ((color8888 >>  3) & 0x0000001F) |
		       ((color8888 >>  6) & 0x000003E0) |
		       ((color8888 >>  9) & 0x00007C00) |
		       ((color8888 >> 16) & 0x00008000);
	}

	/**
	 * Convert a 8888 color in BGR format (GU_COLOR_8888)
	 * to a 565 color in ABGR format (GU_COLOR_5650).
	 *
	 *     8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
	 *                  76543210765432107654321076543210
	 * transformed into
	 *     5650 format: BBBBBGGGGGGRRRRR
	 *                  7654376543276543
	 *
	 * @param color8888 8888 color in BGR format (GU_COLOR_8888)
	 * @return          565 color in ABGR format (GU_COLOR_5650)
	 */
	public static int color8888to565(int color8888) {
		return ((color8888 >> 3) & 0x0000001F) |
		       ((color8888 >> 5) & 0x000007E0) |
		       ((color8888 >> 8) & 0x0000F800);
	}

	private static final class PixelFormat4444Encoder implements IMemoryReaderWriter {
		private IMemoryReaderWriter memoryReaderWriter;

		public PixelFormat4444Encoder(IMemoryReaderWriter memoryReaderWriter) {
			this.memoryReaderWriter = memoryReaderWriter;
		}

		@Override
		public void writeNext(int value) {
			memoryReaderWriter.writeNext(color8888to4444(value));
		}

		@Override
		public void skip(int n) {
			memoryReaderWriter.skip(n);
		}

		@Override
		public void flush() {
			memoryReaderWriter.flush();
		}

		@Override
		public int getCurrentAddress() {
			return memoryReaderWriter.getCurrentAddress();
		}

		@Override
		public int readCurrent() {
			return color4444to8888(memoryReaderWriter.readCurrent());
		}
	}

	private static final class PixelFormat5551Encoder implements IMemoryReaderWriter {
		private IMemoryReaderWriter memoryReaderWriter;

		public PixelFormat5551Encoder(IMemoryReaderWriter memoryReaderWriter) {
			this.memoryReaderWriter = memoryReaderWriter;
		}

		@Override
		public void writeNext(int value) {
			memoryReaderWriter.writeNext(color8888to5551(value));
		}

		@Override
		public void skip(int n) {
			memoryReaderWriter.skip(n);
		}

		@Override
		public void flush() {
			memoryReaderWriter.flush();
		}

		@Override
		public int getCurrentAddress() {
			return memoryReaderWriter.getCurrentAddress();
		}

		@Override
		public int readCurrent() {
			return color5551to8888(memoryReaderWriter.readCurrent());
		}
	}

	private static final class PixelFormat565Encoder implements IMemoryReaderWriter {
		private IMemoryReaderWriter memoryReaderWriter;

		public PixelFormat565Encoder(IMemoryReaderWriter memoryReaderWriter) {
			this.memoryReaderWriter = memoryReaderWriter;
		}

		@Override
		public void writeNext(int value) {
			memoryReaderWriter.writeNext(color8888to565(value));
		}

		@Override
		public void skip(int n) {
			memoryReaderWriter.skip(n);
		}

		@Override
		public void flush() {
			memoryReaderWriter.flush();
		}

		@Override
		public int getCurrentAddress() {
			return memoryReaderWriter.getCurrentAddress();
		}

		@Override
		public int readCurrent() {
			return color565to8888(memoryReaderWriter.readCurrent());
		}
	}
}
