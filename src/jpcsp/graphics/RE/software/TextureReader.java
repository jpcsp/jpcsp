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
	public static IPixelFilter getTextureReader(IRandomTextureAccess textureAccess, GeContext context) {
		IPixelFilter textureReader = null;

		if (context.vinfo.transform2D) {
			textureReader = new TextureReader2D(textureAccess);
		} else {
			textureReader = new TextureReader3D(textureAccess);
		}

		return textureReader;
	}

	private static class TextureReader3D implements IPixelFilter {
		private final IRandomTextureAccess textureAccess;
		private final float width;
		private final float height;

		public TextureReader3D(IRandomTextureAccess textureAccess) {
			this.textureAccess = textureAccess;
			width = textureAccess.getWidth();
			height = textureAccess.getHeight();
		}

		@Override
		public int filter(PixelState pixel) {
			return textureAccess.readPixel(Math.round(pixel.u * width), Math.round(pixel.v * height));
		}
	}

	private static class TextureReader2D implements IPixelFilter {
		private final IRandomTextureAccess textureAccess;

		public TextureReader2D(IRandomTextureAccess textureAccess) {
			this.textureAccess = textureAccess;
		}

		@Override
		public int filter(PixelState pixel) {
			return textureAccess.readPixel(Math.round(pixel.u), Math.round(pixel.v));
		}
	}
}
