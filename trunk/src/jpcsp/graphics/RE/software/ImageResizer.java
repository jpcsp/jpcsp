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
public class ImageResizer {
	protected static final Logger log = VideoEngine.log;

	public static IMemoryReader getImageResizer(IMemoryReader imageReader, int fromWidth, int fromHeight, int toWidth, int toHeight) {
		if (fromWidth != toWidth || fromHeight != toHeight) {
        	if (log.isTraceEnabled()) {
            	log.trace(String.format("Using ImageResizer: fromWidth=%d, fromHeight=%d, toWidth=%d, toHeight=%d", fromWidth, fromHeight, toWidth, toHeight));
            }
			imageReader = new ImageExtender(imageReader, fromWidth, fromHeight, toWidth, toHeight);
		}

		return imageReader;
	}

	private static final class ImageExtender implements IMemoryReader {
		private IMemoryReader imageReader;
		private int currentY;
		private float realY;
		private float stepX;
		private float stepY;
		private int lineLength;
		private int lineIndex;
		private int completeLineSkip;
		private int[] currentPixels;

		public ImageExtender(IMemoryReader imageReader, int fromWidth, int fromHeight, int toWidth, int toHeight) {
			this.imageReader = imageReader;

			stepX = ((float) fromWidth) / toWidth;
			stepY = ((float) fromHeight) / toHeight;
			lineLength = toWidth;
			completeLineSkip = fromWidth;
			currentPixels = new int[lineLength];
			readLine();
		}

		private void readLine() {
			float realX = 0;
			int currentX = 0;
			int currentPixel = imageReader.readNext();
			int read = 1;
			for (int i = 0; i < lineLength; i++) {
				int wantedX = Math.round(realX);
				if (wantedX > currentX) {
					int skip = wantedX - currentX - 1;
					if (skip > 0) {
						imageReader.skip(skip);
						read += skip;
					}
					currentPixel = imageReader.readNext();
					read++;
					currentX = wantedX;
				}
				realX += stepX;
				currentPixels[i] = currentPixel;
			}

			if (read < completeLineSkip) {
				imageReader.skip(completeLineSkip - read);
			}
		}

		@Override
		public int readNext() {
			if (lineIndex >= lineLength) {
				realY += stepY;
				int wantedY = Math.round(realY);
				if (wantedY > currentY) {
					int lineSkip = wantedY - currentY - 1;
					if (lineSkip > 0) {
						imageReader.skip(completeLineSkip * lineSkip);
					}
					readLine();
					currentY = wantedY;
				}
				lineIndex = 0;
			}

			return currentPixels[lineIndex++];
		}

		@Override
		public void skip(int n) {
		}
	}
}
