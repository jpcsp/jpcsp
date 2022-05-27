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

import static jpcsp.graphics.GeCommands.TWRAP_WRAP_MODE_REPEAT;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class TextureClip {
	public static IRandomTextureAccess getTextureClip(GeContext context, int mipmapLevel, IRandomTextureAccess textureAccess, int width, int height) {
		boolean needClipWidth = false;
		boolean needClipHeight = false;

		// No need to clip width if it will be wrapped with "repeat" mode on the required width
		if (context.tex_wrap_s != TWRAP_WRAP_MODE_REPEAT || context.texture_width[mipmapLevel] > width) {
			needClipWidth = true;
		}
		// No need to clip height if it will be wrapped with "repeat" mode on the required height
		if (context.tex_wrap_t != TWRAP_WRAP_MODE_REPEAT || context.texture_height[mipmapLevel] > height) {
			needClipHeight = true;
		}

		if (needClipWidth) {
			if (needClipHeight) {
				textureAccess = new TextureClipWidthHeight(textureAccess, width, height);
			} else {
				textureAccess = new TextureClipWidth(textureAccess, width);
			}
		} else {
			if (needClipHeight) {
				textureAccess = new TextureClipHeight(textureAccess, height);
			}
		}

		return textureAccess;
	}

	private static class TextureClipWidth implements IRandomTextureAccess {
		private IRandomTextureAccess textureAccess;
		private int width;

		public TextureClipWidth(IRandomTextureAccess textureAccess, int width) {
			this.textureAccess = textureAccess;
			this.width = width;
		}

		@Override
		public int readPixel(int u, int v) {
			if (u < 0 || u >= width) {
				return 0;
			}
			return textureAccess.readPixel(u, v);
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

	private static class TextureClipHeight implements IRandomTextureAccess {
		private IRandomTextureAccess textureAccess;
		private int height;

		public TextureClipHeight(IRandomTextureAccess textureAccess, int height) {
			this.textureAccess = textureAccess;
			this.height = height;
		}

		@Override
		public int readPixel(int u, int v) {
			if (v < 0 || v >= height) {
				return 0;
			}
			return textureAccess.readPixel(u, v);
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

	private static class TextureClipWidthHeight implements IRandomTextureAccess {
		private IRandomTextureAccess textureAccess;
		private int width;
		private int height;

		public TextureClipWidthHeight(IRandomTextureAccess textureAccess, int width, int height) {
			this.textureAccess = textureAccess;
			this.width = width;
			this.height = height;
		}

		@Override
		public int readPixel(int u, int v) {
			if (u < 0 || u >= width || v < 0 || v >= height) {
				return 0;
			}
			return textureAccess.readPixel(u, v);
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
