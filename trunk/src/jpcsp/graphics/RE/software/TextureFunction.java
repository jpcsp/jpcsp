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
import jpcsp.memory.IMemoryReader;

/**
 * @author gid15
 *
 */
public class TextureFunction {
	protected static final Logger log = VideoEngine.log;

	public static ISourceReader getTextureFunction(IMemoryReader imageReader, GeContext context) {
		ISourceReader sourceReader = null;

    	if (log.isTraceEnabled()) {
        	log.trace(String.format("Using TextureFunction: func=%d, textureAlphaUsed=%b", context.textureFunc, context.textureAlphaUsed));
        }

    	switch (context.textureFunc) {
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_MODULATE:
				if (context.textureAlphaUsed) {
					sourceReader = new TextureEffectModulateRGBA(imageReader);
				} else {
					sourceReader = new TextureEffectModulateRGB(imageReader);
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_DECAL:
				if (context.textureAlphaUsed) {
					sourceReader = new TextureEffectDecalRGBA(imageReader);
				} else {
					sourceReader = new TextureEffectDecalRGB(imageReader);
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_BLEND:
				if (context.textureAlphaUsed) {
					sourceReader = new TextureEffectBlendRGBA(imageReader, context.tex_env_color);
				} else {
					sourceReader = new TextureEffectBlendRGB(imageReader, context.tex_env_color);
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_REPLACE:
				if (context.textureAlphaUsed) {
					// No transformation applied
					sourceReader = new ImageSourceReader(imageReader);
				} else {
					sourceReader = new TextureEffectReplaceRGB(imageReader);
				}
				break;
			case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_ADD:
				if (context.textureAlphaUsed) {
					sourceReader = new TextureEffectAddRGBA(imageReader);
				} else {
					sourceReader = new TextureEffectAddRGB(imageReader);
				}
				break;
		}

		if (sourceReader == null) {
			sourceReader = new ImageSourceReader(imageReader);
		}

		return sourceReader;
	}

	private static abstract class TextureEffect implements ISourceReader {
		protected IMemoryReader imageReader;
		protected int primaryColorB;
		protected int primaryColorG;
		protected int primaryColorR;

		public TextureEffect(IMemoryReader imageReader) {
			this.imageReader = imageReader;
		}

		public TextureEffect(IMemoryReader imageReader, float[] primaryColor) {
			this.imageReader = imageReader;
			primaryColorB = getColor(primaryColor[2]);
			primaryColorG = getColor(primaryColor[1]);
			primaryColorR = getColor(primaryColor[0]);
		}

		@Override
		public int read(PixelState pixel) {
			int textureColor = imageReader.readNext();
			return applyEffect(pixel, textureColor);
		}

		protected abstract int applyEffect(PixelState pixel, int textureColor);
	}

	private static final class TextureEffectModulateRGB extends TextureEffect {
		public TextureEffectModulateRGB(IMemoryReader imageReader) {
			super(imageReader);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			return multiply(textureColor | 0xFF000000, pixel.primaryColor);
		}
	}

	private static final class TextureEffectModulateRGBA extends TextureEffect {
		public TextureEffectModulateRGBA(IMemoryReader imageReader) {
			super(imageReader);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			return multiply(textureColor, pixel.primaryColor);
		}
	}

	private static final class TextureEffectDecalRGB extends TextureEffect {
		public TextureEffectDecalRGB(IMemoryReader imageReader) {
			super(imageReader);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			return (textureColor & 0x00FFFFFF) | (pixel.primaryColor & 0xFF000000);
		}
	}

	private static final class TextureEffectDecalRGBA extends TextureEffect {
		public TextureEffectDecalRGBA(IMemoryReader imageReader) {
			super(imageReader);
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
		public TextureEffectBlendRGB(IMemoryReader imageReader, float[] primaryColor) {
			super(imageReader, primaryColor);
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
		public TextureEffectBlendRGBA(IMemoryReader imageReader, float[] primaryColor) {
			super(imageReader, primaryColor);
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
		public TextureEffectReplaceRGB(IMemoryReader imageReader) {
			super(imageReader);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			return (textureColor & 0x00FFFFFF) | (pixel.primaryColor & 0xFF000000);
		}
	}

	private static final class TextureEffectAddRGB extends TextureEffect {
		public TextureEffectAddRGB(IMemoryReader imageReader) {
			super(imageReader);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			return add(textureColor & 0x00FFFFFF, pixel.primaryColor);
		}
	}

	private static final class TextureEffectAddRGBA extends TextureEffect {
		public TextureEffectAddRGBA(IMemoryReader imageReader) {
			super(imageReader);
		}

		@Override
		protected int applyEffect(PixelState pixel, int textureColor) {
			int a = multiplyComponent(getAlpha(textureColor), getAlpha(pixel.primaryColor));
			return setAlpha(add(textureColor, pixel.primaryColor), a << 24);
		}
	}
}
