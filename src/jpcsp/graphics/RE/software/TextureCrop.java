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

/**
 * @author gid15
 *
 */
public class TextureCrop {
	protected static final Logger log = VideoEngine.log;

	public static IRandomTextureAccess getImageCrop(IRandomTextureAccess textureAccess, int wantedWidth, int cropLeft, int cropRight, int cropTop) {
		if (cropLeft > 0 || cropTop > 0) {
			cropLeft = Math.max(cropLeft, 0);
			cropRight = Math.max(cropRight, 0);
			cropTop = Math.max(cropTop, 0);
        	if (log.isTraceEnabled()) {
            	log.trace(String.format("Using ImageCrop: wantedWidth=%d, cropLeft=%d, cropRight=%d, cropTop=%d", wantedWidth, cropLeft, cropRight, cropTop));
            }
        	textureAccess = new TextureCropReader(textureAccess, cropLeft, cropTop);
		}

		return textureAccess;
	}

	private static class TextureCropReader implements IRandomTextureAccess {
		protected final IRandomTextureAccess textureAccess;
		protected final int cropLeft;
		protected final int cropTop;

		public TextureCropReader(IRandomTextureAccess textureAccess, int cropLeft, int cropTop) {
			this.textureAccess = textureAccess;
			this.cropLeft = cropLeft;
			this.cropTop = cropTop;
		}

		@Override
		public int readPixel(int u, int v) {
			return textureAccess.readPixel(u + cropLeft, v + cropTop);
		}

		@Override
		public int getWidth() {
			return textureAccess.getWidth();
		}

		@Override
		public int getHeight() {
			return textureAccess.getHeight();
		}
	}
}
