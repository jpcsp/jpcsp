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
public class ImageCrop {
	protected static final Logger log = VideoEngine.log;

	public static IMemoryReader getImageCrop(IMemoryReader imageReader, int wantedWidth, int cropLeft, int cropRight, int cropTop) {
		if (cropLeft > 0 || cropRight > 0 || cropTop > 0) {
			cropLeft = Math.max(cropLeft, 0);
			cropRight = Math.max(cropRight, 0);
			cropTop = Math.max(cropTop, 0);
        	if (log.isTraceEnabled()) {
            	log.trace(String.format("Using ImageCrop: wantedWidth=%d, cropLeft=%d, cropRight=%d, cropTop=%d", wantedWidth, cropLeft, cropRight, cropTop));
            }
			imageReader = new ImageCropReader(imageReader, wantedWidth, cropLeft, cropRight, cropTop);
		}

		return imageReader;
	}

	private static class ImageCropReader implements IMemoryReader {
		protected final IMemoryReader imageReader;
		protected final int totalWidth;
		protected final int cropLeft;
		protected final int xCropRight;
		protected int x;

		public ImageCropReader(IMemoryReader imageReader, int wantedWidth, int cropLeft, int cropRight, int cropTop) {
			this.imageReader = imageReader;
			this.cropLeft = cropLeft;
			totalWidth = cropLeft + wantedWidth + cropRight;
			xCropRight = cropLeft + wantedWidth;

			if (cropTop > 0) {
				imageReader.skip(cropTop * totalWidth);
			}
		}

		@Override
		public int readNext() {
			if (x < cropLeft) {
				imageReader.skip(cropLeft - x);
				x = cropLeft;
			} else if (x >= xCropRight) {
				// Skip to end of line (cropRight) plus
				// skip the beginning of the next line (cropRight)
				imageReader.skip(totalWidth - x + cropLeft);
				x = cropLeft;
			}
			x++;
			return imageReader.readNext();
		}

		@Override
		public void skip(int n) {
			for (int i = 0; i < n; i++) {
				readNext();
			}
		}
	}
}
