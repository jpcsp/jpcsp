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

import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class TextureReader {
	public static IPixelFilter getTextureReader(IRandomTextureAccess textureAccess, GeContext context, int mipmapLevel) {
		IPixelFilter textureReader = null;

		if (context.vinfo.transform2D) {
			textureReader = new TextureReader2D(textureAccess);
		} else {
			// Take the texture width and height, not the buffer width.
			// E.g.: for a texture having
			//          bufferWidth=200 and width=256,
			//       a texture coordinate
			//          u=0.5
			//       has to be interpreted as 128, and not as 100.
			textureReader = new TextureReader3D(textureAccess, context.texture_width[mipmapLevel], context.texture_height[mipmapLevel]);
		}

		return textureReader;
	}

	/**
	 * Transform a pixel coordinate (floating-point value "u" or "v") into
	 * a texel coordinate (integer value to access the texture).
	 *
	 * The texel coordinate is calculated by truncating the floating point value,
	 * not by rounding it. Otherwise transition problems occur at the borders.
	 * E.g. if a texture has a width of 64, valid texel coordinates range
	 * from 0 to 63. 64 is already outside of the texture and should not be
	 * generated when approaching the border to the texture.
	 *
	 * @param coordinate     the pixel coordinate
	 * @return               the texel coordinate
	 */
	private static final int pixelToTexel(float coordinate) {
		return (int) coordinate;
	}

	private static class TextureReader3D implements IPixelFilter {
		private final IRandomTextureAccess textureAccess;
		private final float width;
		private final float height;

		public TextureReader3D(IRandomTextureAccess textureAccess, int width, int height) {
			this.textureAccess = textureAccess;
			this.width = width;
			this.height = height;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
		}
	}

	private static class TextureReader2D implements IPixelFilter {
		private final IRandomTextureAccess textureAccess;

		public TextureReader2D(IRandomTextureAccess textureAccess) {
			this.textureAccess = textureAccess;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u), pixelToTexel(pixel.v));
		}
	}
}
