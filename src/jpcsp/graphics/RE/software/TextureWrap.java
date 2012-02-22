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
import static jpcsp.graphics.RE.software.TextureReader.pixelToTexel;
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
	 * Wrap the value to the range [0..1[ (1 is excluded).
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
	 * @return        the wrapped value in the range [0..1[ (1 is excluded)
	 */
	public static float wrap(float value) {
		if (value >= 0.f) {
			// value == 4.0 -> return 0.0
			// value == 4.1 -> return 0.1
			// value == 4.9 -> return 0.9
			return value - (int) value;
		}

		// value == -4.0 -> return 0.0
		// value == -4.1 -> return 0.9
		// value == -4.9 -> return 0.1
		// value == -1e-8 -> return 0.0
		float wrappedValue = value - (float) Math.floor(value);
		if (wrappedValue >= 1.f) {
			wrappedValue -= 1.f;
		}
		return wrappedValue;
	}

	private static int wrap(float value, int valueMask) {
		return pixelToTexel(value) & valueMask;
	}

	private static final class TextureWrapRepeatST implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.u = wrap(pixel.u);
			pixel.v = wrap(pixel.v);
		}

		@Override
		public int getCompilationId() {
			return 770460230;
		}

		@Override
		public int getFlags() {
			return REQUIRES_TEXTURE_U_V;
		}
	}

	private static final class TextureWrapRepeatS implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.u = wrap(pixel.u);
		}

		@Override
		public int getCompilationId() {
			return 841502345;
		}

		@Override
		public int getFlags() {
			return REQUIRES_TEXTURE_U_V;
		}
	}

	private static final class TextureWrapRepeatT implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.v = wrap(pixel.v);
		}

		@Override
		public int getCompilationId() {
			return 881478743;
		}

		@Override
		public int getFlags() {
			return REQUIRES_TEXTURE_U_V;
		}
	}

	private static final class TextureWrap2DRepeatST implements IPixelFilter {
		private int widthMask;
		private int heightMask;

		public TextureWrap2DRepeatST(int width, int height) {
			widthMask = width - 1;
			heightMask = height - 1;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.u = wrap(pixel.u, widthMask);
			pixel.v = wrap(pixel.v, heightMask);
		}

		@Override
		public int getCompilationId() {
			return 657251522;
		}

		@Override
		public int getFlags() {
			return REQUIRES_TEXTURE_U_V;
		}
	}

	private static final class TextureWrap2DRepeatS implements IPixelFilter {
		private int widthMask;

		public TextureWrap2DRepeatS(int width) {
			widthMask = width - 1;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.u = wrap(pixel.u, widthMask);
		}

		@Override
		public int getCompilationId() {
			return 233194390;
		}

		@Override
		public int getFlags() {
			return REQUIRES_TEXTURE_U_V;
		}
	}

	private static final class TextureWrap2DRepeatT implements IPixelFilter {
		private int heightMask;

		public TextureWrap2DRepeatT(int height) {
			heightMask = height - 1;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.v = wrap(pixel.v, heightMask);
		}

		@Override
		public int getCompilationId() {
			return 42874867;
		}

		@Override
		public int getFlags() {
			return REQUIRES_TEXTURE_U_V;
		}
	}
}
