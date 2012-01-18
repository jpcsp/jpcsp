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
package jpcsp.graphics.RE.software;

import static jpcsp.graphics.GeCommands.TWRAP_WRAP_MODE_REPEAT;
import static jpcsp.util.Utilities.round;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class TextureWrap {
	public static IPixelFilter getTextureWrap(GeContext context, int mipmapLevel) {
		IPixelFilter textureWrap = null;

		if (context.vinfo.transform2D) {
			if (context.tex_wrap_s == TWRAP_WRAP_MODE_REPEAT) {
				if (context.tex_wrap_t == TWRAP_WRAP_MODE_REPEAT) {
					textureWrap = new TextureWrap2DRepeatST(context.texture_width[mipmapLevel], context.texture_height[mipmapLevel]);
				} else {
					textureWrap = new TextureWrap2DRepeatS(context.texture_width[mipmapLevel]);
				}
			} else {
				if (context.tex_wrap_t == TWRAP_WRAP_MODE_REPEAT) {
					textureWrap = new TextureWrap2DRepeatT(context.texture_height[mipmapLevel]);
				}
			}
		} else {
			if (context.tex_wrap_s == TWRAP_WRAP_MODE_REPEAT) {
				if (context.tex_wrap_t == TWRAP_WRAP_MODE_REPEAT) {
					textureWrap = new TextureWrapRepeatST();
				} else {
					textureWrap = new TextureWrapRepeatS();
				}
			} else {
				if (context.tex_wrap_t == TWRAP_WRAP_MODE_REPEAT) {
					textureWrap = new TextureWrapRepeatT();
				}
			}
		}

		return textureWrap;
	}

	/**
	 * Wrap the value to the range [0..1].
	 *
	 * E.g.
	 *    value == 4.0 -> return 0.0
	 *    value == 4.1 -> return 0.1
	 *    value == 4.9 -> return 0.9
	 *    value == -4.0 -> return 0.0
	 *    value == -4.1 -> return 0.9 (and not 0.1)
	 *    value == -4.9 -> return 0.1 (and not 0.9)
	 *
	 * @param value   the value to be wrapped
	 * @return        the wrapped value in the range [0..1]
	 */
	private static float wrap(float value) {
		if (value >= 0.f) {
			// value == 4.0 -> return 0.0
			// value == 4.1 -> return 0.1
			// value == 4.9 -> return 0.9
			return value - (int) value;
		}
		// value == -4.0 -> return 0.0
		// value == -4.1 -> return 0.9
		// value == -4.9 -> return 0.1
		return value - (float) Math.floor(value);
	}

	private static int wrap(float value, int valueMask) {
		return round(value) & valueMask;
	}

	private static class TextureWrapRepeatST implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			pixel.u = wrap(pixel.u);
			pixel.v = wrap(pixel.v);
			return pixel.source;
		}
	}

	private static class TextureWrapRepeatS implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			pixel.u = wrap(pixel.u);
			return pixel.source;
		}
	}

	private static class TextureWrapRepeatT implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			pixel.v = wrap(pixel.v);
			return pixel.source;
		}
	}

	private static class TextureWrap2DRepeatST implements IPixelFilter {
		private int widthMask;
		private int heightMask;

		public TextureWrap2DRepeatST(int width, int height) {
			widthMask = width - 1;
			heightMask = height - 1;
		}

		@Override
		public int filter(PixelState pixel) {
			pixel.u = wrap(pixel.u, widthMask);
			pixel.v = wrap(pixel.v, heightMask);
			return pixel.source;
		}
	}

	private static class TextureWrap2DRepeatS implements IPixelFilter {
		private int widthMask;

		public TextureWrap2DRepeatS(int width) {
			widthMask = width - 1;
		}

		@Override
		public int filter(PixelState pixel) {
			pixel.u = wrap(pixel.u, widthMask);
			return pixel.source;
		}
	}

	private static class TextureWrap2DRepeatT implements IPixelFilter {
		private int heightMask;

		public TextureWrap2DRepeatT(int height) {
			heightMask = height - 1;
		}

		@Override
		public int filter(PixelState pixel) {
			pixel.v = wrap(pixel.v, heightMask);
			return pixel.source;
		}
	}
}
