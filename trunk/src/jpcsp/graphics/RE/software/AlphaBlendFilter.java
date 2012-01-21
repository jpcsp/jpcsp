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

import static jpcsp.graphics.RE.software.PixelColor.ONE;
import static jpcsp.graphics.RE.software.PixelColor.absBGR;
import static jpcsp.graphics.RE.software.PixelColor.addBGR;
import static jpcsp.graphics.RE.software.PixelColor.getAlpha;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.graphics.RE.software.PixelColor.getColorBGR;
import static jpcsp.graphics.RE.software.PixelColor.minBGR;
import static jpcsp.graphics.RE.software.PixelColor.multiplyBGR;
import static jpcsp.graphics.RE.software.PixelColor.setBGR;
import static jpcsp.graphics.RE.software.PixelColor.substractBGR;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class AlphaBlendFilter {
	public static IPixelFilter getAlphaBlendFilter(GeContext context) {
		IBlendFactor sourceFactor = getFactorFilter(context, context.blend_src, true);
		IBlendFactor destinationFactor = getFactorFilter(context, context.blend_dst, false);
		IPixelFilter filter = null;

		switch (context.blendEquation) {
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_ADD:
				if (context.blend_src == GeCommands.ALPHA_FIX && context.sfix == 0xFFFFFF &&
				    context.blend_dst == GeCommands.ALPHA_FIX && context.dfix == 0x000000) {
					filter = NopFilter.NOP;
				} else {
					filter = new BlendOperationAdd(sourceFactor, destinationFactor);
				}
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_SUBTRACT:
				filter = new BlendOperationSubstract(sourceFactor, destinationFactor);
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_REVERSE_SUBTRACT:
				filter = new BlendOperationReverseSubstract(sourceFactor, destinationFactor);
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_MINIMUM_VALUE:
				filter = new BlendOperationMin(sourceFactor, destinationFactor);
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_MAXIMUM_VALUE:
				filter = new BlendOperationMax(sourceFactor, destinationFactor);
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE:
				filter = new BlendOperationAbs(sourceFactor, destinationFactor);
				break;
		}

		return filter;
	}

	private static IBlendFactor getFactorFilter(GeContext context, int factor, boolean isSrc) {
		IBlendFactor filter = null;

		switch (factor) {
			case GeCommands.ALPHA_SOURCE_COLOR:
				filter = isSrc ? new BlendFactorSrcColor() : new BlendFactorDstColor();
				break;
			case GeCommands.ALPHA_ONE_MINUS_SOURCE_COLOR:
				filter = isSrc ? new BlendFactorOneMinusSrcColor() : new BlendFactorOneMinusDstColor();
				break;
			case GeCommands.ALPHA_SOURCE_ALPHA:
				filter = new BlendFactorSrcAlpha();
				break;
			case GeCommands.ALPHA_ONE_MINUS_SOURCE_ALPHA:
				filter = new BlendFactorOneMinusSrcAlpha();
				break;
			case GeCommands.ALPHA_DESTINATION_ALPHA:
				filter = new BlendFactorDstAlpha();
				break;
			case GeCommands.ALPHA_ONE_MINUS_DESTINATION_ALPHA:
				filter = new BlendFactorOneMinusDstAlpha();
				break;
			case GeCommands.ALPHA_DOUBLE_SOURCE_ALPHA:
				filter = new BlendFactorDoubleSrcAlpha();
				break;
			case GeCommands.ALPHA_ONE_MINUS_DOUBLE_SOURCE_ALPHA:
				filter = new BlendFactorOneMinusDoubleSrcAlpha();
				break;
			case GeCommands.ALPHA_DOUBLE_DESTINATION_ALPHA:
				filter = new BlendFactorDoubleDstAlpha();
				break;
			case GeCommands.ALPHA_ONE_MINUS_DOUBLE_DESTINATION_ALPHA:
				filter = new BlendFactorOneMinusDoubleDstAlpha();
				break;
			case GeCommands.ALPHA_FIX:
				filter = isSrc ? new BlendFactorFix(context.sfix_color) : new BlendFactorFix(context.dfix_color);
				break;
		}

		return filter;
	}

	private interface IBlendFactor {
		public int getFactor(PixelState pixel);
	}

	private static class BlendFactorSrcColor implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			return pixel.source;
		}
	}

	private static class BlendFactorOneMinusSrcColor implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			return 0xFFFFFFFF - pixel.source;
		}
	}

	private static class BlendFactorDstColor implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			return pixel.destination;
		}
	}

	private static class BlendFactorOneMinusDstColor implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			return 0xFFFFFFFF - pixel.destination;
		}
	}

	private static class BlendFactorSrcAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = getAlpha(pixel.source);
			return getColorBGR(alpha, alpha, alpha);
		}
	}

	private static class BlendFactorOneMinusSrcAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = ONE - getAlpha(pixel.source);
			return getColorBGR(alpha, alpha, alpha);
		}
	}

	private static class BlendFactorDstAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = getAlpha(pixel.destination);
			return getColorBGR(alpha, alpha, alpha);
		}
	}

	private static class BlendFactorOneMinusDstAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = ONE - getAlpha(pixel.destination);
			return getColorBGR(alpha, alpha, alpha);
		}
	}

	private static class BlendFactorDoubleSrcAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = getAlpha(pixel.source) << 1;
			return getColorBGR(alpha, alpha, alpha);
		}
	}

	private static class BlendFactorOneMinusDoubleSrcAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = ONE - (getAlpha(pixel.source) << 1);
			return getColorBGR(alpha, alpha, alpha);
		}
	}

	private static class BlendFactorDoubleDstAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = getAlpha(pixel.destination) << 1;
			return getColorBGR(alpha, alpha, alpha);
		}
	}

	private static class BlendFactorOneMinusDoubleDstAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = ONE - (getAlpha(pixel.destination) << 1);
			return getColorBGR(alpha, alpha, alpha);
		}
	}

	private static class BlendFactorFix implements IBlendFactor {
		private int fixColor;

		public BlendFactorFix(float[] color) {
			fixColor = getColor(color) & 0x00FFFFFF;
		}

		@Override
		public int getFactor(PixelState pixel) {
			return fixColor;
		}
	}

	private static abstract class BlendOperation implements IPixelFilter {
		protected IBlendFactor sourceFactor;
		protected IBlendFactor destinationFactor;

		public BlendOperation(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			this.sourceFactor = sourceFactor;
			this.destinationFactor = destinationFactor;
		}
	}

	private static final class BlendOperationAdd extends BlendOperation {
		public BlendOperationAdd(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public void filter(PixelState pixel) {
			int filteredSource = multiplyBGR(pixel.source, sourceFactor.getFactor(pixel));
			int filteredDestination = multiplyBGR(pixel.destination, destinationFactor.getFactor(pixel));
			pixel.source = setBGR(pixel.source, addBGR(filteredSource, filteredDestination));
		}
	}

	private static final class BlendOperationSubstract extends BlendOperation {
		public BlendOperationSubstract(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public void filter(PixelState pixel) {
			int filteredSource = multiplyBGR(pixel.source, sourceFactor.getFactor(pixel));
			int filteredDestination = multiplyBGR(pixel.destination, destinationFactor.getFactor(pixel));
			pixel.source = setBGR(pixel.source, substractBGR(filteredSource, filteredDestination));
		}
	}

	private static final class BlendOperationReverseSubstract extends BlendOperation {
		public BlendOperationReverseSubstract(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public void filter(PixelState pixel) {
			int filteredSource = multiplyBGR(pixel.source, sourceFactor.getFactor(pixel));
			int filteredDestination = multiplyBGR(pixel.destination, destinationFactor.getFactor(pixel));
			pixel.source = setBGR(pixel.source, substractBGR(filteredDestination, filteredSource));
		}
	}

	private static final class BlendOperationMin extends BlendOperation {
		public BlendOperationMin(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public void filter(PixelState pixel) {
			// Source and destination factors are not applied
			pixel.source = setBGR(pixel.source, minBGR(pixel.source, pixel.destination));
		}
	}

	private static final class BlendOperationMax extends BlendOperation {
		public BlendOperationMax(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public void filter(PixelState pixel) {
			// Source and destination factors are not applied
			pixel.source = setBGR(pixel.source, minBGR(pixel.source, pixel.destination));
		}
	}

	private static final class BlendOperationAbs extends BlendOperation {
		public BlendOperationAbs(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public void filter(PixelState pixel) {
			// Source and destination factors are not applied
			pixel.source = setBGR(pixel.source, absBGR(pixel.source, pixel.destination));
		}
	}
}
