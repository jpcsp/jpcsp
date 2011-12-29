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

import org.apache.log4j.Logger;

import jpcsp.graphics.VideoEngine;
import jpcsp.memory.IMemoryReader;

/**
 * @author gid15
 *
 */
public class ImageFlip {
	protected static final Logger log = VideoEngine.log;

	public static IMemoryReader getImageFlip(IMemoryReader imageReader, int width, int height, boolean flipX, boolean flipY, boolean rotate) {
		if (flipX) {
            if (flipY) {
				// TODO
	            if (log.isTraceEnabled()) {
	            	log.trace(String.format("Using ImageFlipXY: width=%d, height=%d", width, height));
	            }
			} else {
	            if (log.isTraceEnabled()) {
	            	log.trace(String.format("Using ImageFlipX: width=%d, height=%d", width, height));
	            }
				imageReader = new ImageFlipX(imageReader, width);
			}
		} else {
			if (flipY) {
	            if (log.isTraceEnabled()) {
	            	log.trace(String.format("Using ImageFlipY: width=%d, height=%d", width, height));
	            }
	            imageReader = new ImageFlipY(imageReader, width, height);
			}
		}

		if (rotate) {
			imageReader = new ImageRotate(imageReader, width, height);
		}

		return imageReader;
	}

	private static final class ImageFlipX implements IMemoryReader {
		protected int width;
		protected IMemoryReader imageReader;
		protected int[] line;
		protected int x;

		public ImageFlipX(IMemoryReader imageReader, int width) {
			this.imageReader = imageReader;
			this.width = width;
			line = new int[width];
		}

		protected void readLine() {
			for (int i = 0; i < width; i++) {
				line[i] = imageReader.readNext();
			}
		}

		@Override
		public int readNext() {
			if (x <= 0) {
				readLine();
				x = width;
			}

			x--;
			return line[x];
		}

		@Override
		public void skip(int n) {
			int lines = n / width;
			n = n % width;
			x -= n % width;
			if (x <= 0) {
				lines++;
				x += width;
			}

			if (lines > 0) {
				if (lines > 1) {
					imageReader.skip((lines - 1) * width);
				}
				readLine();
			}
		}
	}

	private static abstract class ImageCompleteReader implements IMemoryReader {
		private IMemoryReader imageReader;
		private int width;
		private int height;
		private int[] image;
		private int index;

		public ImageCompleteReader(IMemoryReader imageReader, int width, int height) {
			this.imageReader = imageReader;
			this.width = width;
			this.height = height;
		}

		@Override
		public int readNext() {
			if (image == null) {
				image = new int[height * width];
				readImage(imageReader, image, width, height);
			}
			return image[index++];
		}

		@Override
		public void skip(int n) {
			index += n;
		}

		abstract protected void readImage(IMemoryReader imageReader, int[] image, int width, int height);
	}

	private static final class ImageFlipY extends ImageCompleteReader {
		public ImageFlipY(IMemoryReader imageReader, int width, int height) {
			super(imageReader, width, height);
		}

		@Override
		protected void readImage(IMemoryReader imageReader, int[] image, int width, int height) {
			int index;
			for (int y = 0; y < height; y++) {
				index = (height - y - 1) * width;
				for (int x = 0; x < width; x++) {
					image[index++] = imageReader.readNext();
				}
			}
		}
	}

	private static final class ImageRotate extends ImageCompleteReader {
		public ImageRotate(IMemoryReader imageReader, int width, int height) {
			super(imageReader, width, height);
		}

		@Override
		protected void readImage(IMemoryReader imageReader, int[] image, int width, int height) {
			int index;
			for (int y = 0; y < height; y++) {
				index = y;
				for (int x = 0; x < width; x++) {
					image[index] = imageReader.readNext();
					index += height;
				}
			}
		}
	}
}
