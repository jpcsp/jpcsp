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

import org.apache.log4j.Logger;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class TextureMapping {
	protected static final Logger log = VideoEngine.log;

	public static IPixelFilter getTextureMapping(GeContext context) {
		IPixelFilter textureMapping = null;

		switch (context.tex_map_mode) {
			case GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV:
				if (context.tex_scale_x != 1.f || context.tex_scale_y != 1.f || context.tex_translate_x != 0.f || context.tex_translate_y != 0.f) {
					textureMapping = new TextureMapUV(context.tex_scale_x, context.tex_scale_y, context.tex_translate_x, context.tex_translate_y);
				}
				break;
			case GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX:
				log.warn("TextureMapping TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX not yet implemented");
				break;
			case GeCommands.TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP:
				log.warn("TextureMapping TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP not yet implemented");
				break;
		}

		return textureMapping;
	}

	private static class TextureMapUV implements IPixelFilter {
		private float scaleU;
		private float scaleV;
		private float translateU;
		private float translateV;

		public TextureMapUV(float scaleU, float scaleV, float translateU, float translateV) {
			this.scaleU = scaleU;
			this.scaleV = scaleV;
			this.translateU = translateU;
			this.translateV = translateV;
		}

		@Override
		public int filter(PixelState pixel) {
			pixel.u = pixel.u * scaleU + translateU;
			pixel.v = pixel.v * scaleV + translateV;
			return pixel.source;
		}
	}
}
