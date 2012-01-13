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

	public static IPixelFilter getTextureFunction(IRandomTextureAccess textureAccess, GeContext context) {
		IPixelFilter textureFunction = null;

    	if (log.isTraceEnabled()) {
        	log.trace(String.format("Using TextureFunction: func=%d, textureAlphaUsed=%b", context.textureFunc, context.textureAlphaUsed));
        }

    	switch (context.textureFunc) {
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_MODULATE:
				if (context.textureAlphaUsed) {
					textureFunction = new TextureEffectModulateRGBA(textureAccess);
				} else {
					textureFunction = new TextureEffectModulateRGB(textureAccess);
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_DECAL:
				if (context.textureAlphaUsed) {
					textureFunction = new TextureEffectDecalRGBA(textureAccess);
				} else {
					textureFunction = new TextureEffectDecalRGB(textureAccess);
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_BLEND:
				if (context.textureAlphaUsed) {
					textureFunction = new TextureEffectBlendRGBA(textureAccess, context.tex_env_color);
				} else {
					textureFunction = new TextureEffectBlendRGB(textureAccess, context.tex_env_color);
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_REPLACE:
				if (context.textureAlphaUsed) {
					// No transformation applied
					textureFunction = new NopTextureFilter(textureAccess);
				} else {
					textureFunction = new TextureEffectReplaceRGB(textureAccess);
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_ADD:
				if (context.textureAlphaUsed) {
					textureFunction = new TextureEffectAddRGBA(textureAccess);
				} else {
					textureFunction = new TextureEffectAddRGB(textureAccess);
				}
				break;
		}

		if (textureFunction == null) {
			textureFunction = new NopTextureFilter(textureAccess);
		}

		return textureFunction;
	}

	private static abstract class TextureEffect implements IPixelFilter {
		protected IRandomTextureAccess textureAccess;
		protected int primaryColorB;
		protected int primaryColorG;
		protected int primaryColorR;

		public TextureEffect(IRandomTextureAccess textureAccess) {
			this.textureAccess = textureAccess;
		}

		public TextureEffect(IRandomTextureAccess textureAccess, float[] primaryColor) {
			this.textureAccess = textureAccess;
			primaryColorB = getColor(primaryColor[2]);
			primaryColorG = getColor(primaryColor[1]);
			primaryColorR = getColor(primaryColor[0]);
		}

		@Override
		public int filter(PixelState pixel) {
			int textureColor = textureAccess.readPixel(pixel.u, pixel.v);
			return applyEffect(pixel, textureColor);
		}

		protected abstract int applyEffect(PixelState pixel, int textureColor);
	}

	private static final class TextureEffectModulateRGB extends TextureEffect {
		public TextureEffectModulateRGB(IRandomTextureAccess textureAccess) {
			super(textureAccess);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			return multiply(textureColor | 0xFF000000, pixel.primaryColor);
		}
	}

	private static final class TextureEffectModulateRGBA extends TextureEffect {
		public TextureEffectModulateRGBA(IRandomTextureAccess textureAccess) {
			super(textureAccess);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			return multiply(textureColor, pixel.primaryColor);
		}
	}

	private static final class TextureEffectDecalRGB extends TextureEffect {
		public TextureEffectDecalRGB(IRandomTextureAccess textureAccess) {
			super(textureAccess);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			return (textureColor & 0x00FFFFFF) | (pixel.primaryColor & 0xFF000000);
		}
	}

	private static final class TextureEffectDecalRGBA extends TextureEffect {
		public TextureEffectDecalRGBA(IRandomTextureAccess textureAccess) {
			super(textureAccess);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			int alpha = getAlpha(textureColor);
			int a = getAlpha(pixel.primaryColor);
			int b = combineComponent(getBlue(pixel.primaryColor), getBlue(textureColor), alpha);
			int g = combineComponent(getGreen(pixel.primaryColor), getGreen(textureColor), alpha);
			int r = combineComponent(getRed(pixel.primaryColor), getRed(textureColor), alpha);
			return getColor(a, b, g, r);
		}
	}

	private static final class TextureEffectBlendRGB extends TextureEffect {
		public TextureEffectBlendRGB(IRandomTextureAccess textureAccess, float[] primaryColor) {
			super(textureAccess, primaryColor);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			int a = getAlpha(pixel.primaryColor);
			int b = combineComponent(getBlue(pixel.primaryColor), primaryColorB, getBlue(textureColor));
			int g = combineComponent(getGreen(pixel.primaryColor), primaryColorG, getGreen(textureColor));
			int r = combineComponent(getRed(pixel.primaryColor), primaryColorR, getRed(textureColor));
			return getColor(a, b, g, r);
		}
	}

	private static final class TextureEffectBlendRGBA extends TextureEffect {
		public TextureEffectBlendRGBA(IRandomTextureAccess textureAccess, float[] primaryColor) {
			super(textureAccess, primaryColor);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			int a = multiplyComponent(getAlpha(textureColor), getAlpha(pixel.primaryColor));
			int b = combineComponent(getBlue(pixel.primaryColor), primaryColorB, getBlue(textureColor));
			int g = combineComponent(getGreen(pixel.primaryColor), primaryColorG, getGreen(textureColor));
			int r = combineComponent(getRed(pixel.primaryColor), primaryColorR, getRed(textureColor));
			return getColor(a, b, g, r);
		}
	}

	private static final class TextureEffectReplaceRGB extends TextureEffect {
		public TextureEffectReplaceRGB(IRandomTextureAccess textureAccess) {
			super(textureAccess);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			return (textureColor & 0x00FFFFFF) | (pixel.primaryColor & 0xFF000000);
		}
	}

	private static final class TextureEffectAddRGB extends TextureEffect {
		public TextureEffectAddRGB(IRandomTextureAccess textureAccess) {
			super(textureAccess);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			return add(textureColor & 0x00FFFFFF, pixel.primaryColor);
		}
	}

	private static final class TextureEffectAddRGBA extends TextureEffect {
		public TextureEffectAddRGBA(IRandomTextureAccess textureAccess) {
			super(textureAccess);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			int a = multiplyComponent(getAlpha(textureColor), getAlpha(pixel.primaryColor));
			return setAlpha(add(textureColor, pixel.primaryColor), a << 24);
		}
	}
}
