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

import static jpcsp.graphics.RE.software.PixelColor.add;
import static jpcsp.graphics.RE.software.PixelColor.addBGR;
import static jpcsp.graphics.RE.software.PixelColor.combineComponent;
import static jpcsp.graphics.RE.software.PixelColor.getAlpha;
import static jpcsp.graphics.RE.software.PixelColor.getBlue;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.graphics.RE.software.PixelColor.getGreen;
import static jpcsp.graphics.RE.software.PixelColor.getRed;
import static jpcsp.graphics.RE.software.PixelColor.multiply;
import static jpcsp.graphics.RE.software.PixelColor.multiplyComponent;
import static jpcsp.graphics.RE.software.PixelColor.setAlpha;

import org.apache.log4j.Logger;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class TextureFunction {
	protected static final Logger log = VideoEngine.log;

	public static IPixelFilter getTextureFunction(GeContext context) {
		IPixelFilter textureFunction = null;

    	if (log.isTraceEnabled()) {
        	log.trace(String.format("Using TextureFunction: func=%d, textureAlphaUsed=%b", context.textureFunc, context.textureAlphaUsed));
        }

    	switch (context.textureFunc) {
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_MODULATE:
				if (context.textureAlphaUsed) {
					textureFunction = new TextureEffectModulateRGBA();
				} else {
					textureFunction = new TextureEffectModulateRGB();
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_DECAL:
				if (context.textureAlphaUsed) {
					textureFunction = new TextureEffectDecalRGBA();
				} else {
					textureFunction = new TextureEffectDecalRGB();
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_BLEND:
				if (context.textureAlphaUsed) {
					textureFunction = new TextureEffectBlendRGBA(context.tex_env_color);
				} else {
					textureFunction = new TextureEffectBlendRGB(context.tex_env_color);
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_REPLACE:
				if (context.textureAlphaUsed) {
					// No transformation applied
				} else {
					textureFunction = new TextureEffectReplaceRGB();
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_ADD:
				if (context.textureAlphaUsed) {
					textureFunction = new TextureEffectAddRGBA();
				} else {
					textureFunction = new TextureEffectAddRGB();
				}
				break;
		}

		return textureFunction;
	}

	private static final class TextureEffectModulateRGB implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = multiply(pixel.source | 0xFF000000, pixel.primaryColor);
		}

		@Override
		public int getCompilationId() {
			return 628774010;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class TextureEffectModulateRGBA implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = multiply(pixel.source, pixel.primaryColor);
		}

		@Override
		public int getCompilationId() {
			return 755309990;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class TextureEffectDecalRGB implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = (pixel.source & 0x00FFFFFF) | (pixel.primaryColor & 0xFF000000);
		}

		@Override
		public int getCompilationId() {
			return 498437966;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class TextureEffectDecalRGBA implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			int alpha = getAlpha(pixel.source);
			int a = getAlpha(pixel.primaryColor);
			int b = combineComponent(getBlue(pixel.primaryColor), getBlue(pixel.source), alpha);
			int g = combineComponent(getGreen(pixel.primaryColor), getGreen(pixel.source), alpha);
			int r = combineComponent(getRed(pixel.primaryColor), getRed(pixel.source), alpha);
			pixel.source = getColor(a, b, g, r);
		}

		@Override
		public int getCompilationId() {
			return 938842832;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class TextureEffectBlendRGB implements IPixelFilter {
		protected int primaryColorR;
		protected int primaryColorG;
		protected int primaryColorB;

		public TextureEffectBlendRGB(float[] primaryColor) {
			primaryColorR = getColor(primaryColor[0]);
			primaryColorG = getColor(primaryColor[1]);
			primaryColorB = getColor(primaryColor[2]);
		}

		@Override
		public void filter(PixelState pixel) {
			int a = getAlpha(pixel.primaryColor);
			int b = combineComponent(getBlue(pixel.primaryColor), primaryColorB, getBlue(pixel.source));
			int g = combineComponent(getGreen(pixel.primaryColor), primaryColorG, getGreen(pixel.source));
			int r = combineComponent(getRed(pixel.primaryColor), primaryColorR, getRed(pixel.source));
			pixel.source = getColor(a, b, g, r);
		}

		@Override
		public int getCompilationId() {
			return 48392698;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class TextureEffectBlendRGBA implements IPixelFilter {
		protected int primaryColorB;
		protected int primaryColorG;
		protected int primaryColorR;

		public TextureEffectBlendRGBA(float[] primaryColor) {
			primaryColorB = getColor(primaryColor[2]);
			primaryColorG = getColor(primaryColor[1]);
			primaryColorR = getColor(primaryColor[0]);
		}

		@Override
		public void filter(PixelState pixel) {
			int a = multiplyComponent(getAlpha(pixel.source), getAlpha(pixel.primaryColor));
			int b = combineComponent(getBlue(pixel.primaryColor), primaryColorB, getBlue(pixel.source));
			int g = combineComponent(getGreen(pixel.primaryColor), primaryColorG, getGreen(pixel.source));
			int r = combineComponent(getRed(pixel.primaryColor), primaryColorR, getRed(pixel.source));
			pixel.source = getColor(a, b, g, r);
		}

		@Override
		public int getCompilationId() {
			return 840921149;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class TextureEffectReplaceRGB implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = (pixel.source & 0x00FFFFFF) | (pixel.primaryColor & 0xFF000000);
		}

		@Override
		public int getCompilationId() {
			return 376875748;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class TextureEffectAddRGB implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = add(pixel.source & 0x00FFFFFF, pixel.primaryColor);
		}

		@Override
		public int getCompilationId() {
			return 69146893;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class TextureEffectAddRGBA implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			int a = multiplyComponent(getAlpha(pixel.source), getAlpha(pixel.primaryColor));
			pixel.source = setAlpha(addBGR(pixel.source, pixel.primaryColor), a);
		}

		@Override
		public int getCompilationId() {
			return 594528533;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}
}
