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
public class TextureFlip {
	protected static final Logger log = VideoEngine.log;

	public static IRandomTextureAccess getImageFlip(IRandomTextureAccess textureAccess, int width, int height, boolean flipX, boolean flipY, boolean rotate) {
		if (flipX) {
            if (flipY) {
				// TODO
	            if (log.isTraceEnabled()) {
	            	log.trace(String.format("Using ImageFlipXY: width=%d, height=%d", width, height));
	            }
	            textureAccess = new TextureFlipXY(textureAccess, width, height);
			} else {
	            if (log.isTraceEnabled()) {
	            	log.trace(String.format("Using ImageFlipX: width=%d, height=%d", width, height));
	            }
	            textureAccess = new TextureFlipX(textureAccess, width);
			}
		} else {
			if (flipY) {
	            if (log.isTraceEnabled()) {
	            	log.trace(String.format("Using ImageFlipY: width=%d, height=%d", width, height));
	            }
	            textureAccess = new TextureFlipY(textureAccess, height);
			}
		}

		if (rotate) {
            if (log.isTraceEnabled()) {
            	log.trace(String.format("Using ImageRotate: width=%d, height=%d", width, height));
            }
            textureAccess = new TextureRotate(textureAccess);
		}

		return textureAccess;
	}

	private static final class TextureFlipX implements IRandomTextureAccess {
		protected int width;
		protected IRandomTextureAccess textureAccess;

		public TextureFlipX(IRandomTextureAccess textureAccess, int width) {
			this.textureAccess = textureAccess;
			this.width = width;
		}

		@Override
		public int readPixel(int u, int v) {
			return textureAccess.readPixel(width - u, v);
		}
	}

	private static final class TextureFlipY implements IRandomTextureAccess {
		protected int height;
		protected IRandomTextureAccess textureAccess;

		public TextureFlipY(IRandomTextureAccess textureAccess, int height) {
			this.textureAccess = textureAccess;
			this.height = height;
		}

		@Override
		public int readPixel(int u, int v) {
			return textureAccess.readPixel(u, height - v);
		}
	}

	private static final class TextureFlipXY implements IRandomTextureAccess {
		protected int width;
		protected int height;
		protected IRandomTextureAccess textureAccess;

		public TextureFlipXY(IRandomTextureAccess textureAccess, int width, int height) {
			this.textureAccess = textureAccess;
			this.width = width;
			this.height = height;
		}

		@Override
		public int readPixel(int u, int v) {
			return textureAccess.readPixel(width - u, height - v);
		}
	}

	private static final class TextureRotate implements IRandomTextureAccess {
		protected IRandomTextureAccess textureAccess;

		public TextureRotate(IRandomTextureAccess textureAccess) {
			this.textureAccess = textureAccess;
		}

		@Override
		public int readPixel(int u, int v) {
			return textureAccess.readPixel(v, u);
		}
	}
}
